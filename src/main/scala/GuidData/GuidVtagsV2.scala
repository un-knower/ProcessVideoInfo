package GuidData

import Utils.Tools.KeyValueWeight
import Utils.{Defines, TestRedisPool, Tools}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by baronfeng on 2017/9/18.
  */
object GuidVtagsV2 {
  val REPARTITION_NUM = 400
  private val now = System.currentTimeMillis / 1000


  val sv_weight: UserDefinedFunction = udf((tag_weight: Seq[Row], weight: Double) => {
    tag_weight.map(tw => (tw.getString(0), tw.getDouble(1) * weight))
  })

  /**
    * 获取天级别的guid_vtags数据
    * @param spark 目前运行的spark_session
    * @param guid_input_path
    * @param vid_all_tags_path
    * @param output_path
    */
  def guid_vid_sv_join_daily(spark: SparkSession, guid_input_path: String, vid_all_tags_path:String, output_path: String) : Unit = {
    println("begin to get guid_vid_sv.")
    println("guid_input_path: " + guid_input_path)
    println("vid_all_tags_path: " + vid_all_tags_path)

    import spark.implicits._

    // guid, ts, vid, playduration, duration, percent
    val guid_data = spark.read.parquet(guid_input_path).select($"guid", $"vid", $"percent")
      .repartition(REPARTITION_NUM, $"vid").cache

    val vid_tag_weight_data = spark.read.parquet(vid_all_tags_path).as[KeyValueWeight].toDF("vid", "tag_weight")
      .repartition(REPARTITION_NUM, $"vid").cache()

    val guid_result_temp = guid_data.join(vid_tag_weight_data, "vid")
    //  .filter($"vid2".isNotNull && $"vid2" =!= "")
      .filter($"tag_weight".isNotNull)
      .select($"guid", sv_weight($"tag_weight", $"percent") as "tag_weight")
      .repartition(REPARTITION_NUM)

    val res_with_pair = guid_result_temp.flatMap(line => {
      val guid = line.getString(0)
      //      val vid = line.getString(1)
      val tag_weight = if(line.isNullAt(1) || line.getSeq[Row](1).isEmpty) {
        Seq(("", 0.0))
      } else {
        line.getSeq[Row](1).map(row => (row.getString(0), row.getDouble(1)))
      }
      tag_weight.map(tw => {
        (guid, tw._1, tw._2)
      })
    }).filter(_._2.nonEmpty)
      .map(line =>(line._1, line._2.toLong, line._3))
      .toDF("guid", "tagid", "weight")
      .groupBy($"guid", $"tagid").agg(sum($"weight") as "weight")
      .filter($"weight" > 0.01)
      .cache()


    println(s"write to parquet, to path: $output_path, number: ${res_with_pair.count}")
    res_with_pair.write.mode("overwrite").parquet(output_path)
    println("--------[get guid tags done]--------")

  }


  val tuple2: UserDefinedFunction = udf((l: Long, d: Double) => (l, d))
  // 目前monthly去掉vids
  /**
    * 产生本月的所有数据，产生的数量由tools中的monthly确定，务必确认一下，目前视数据规模定为10天和30天，之后可能会扩展为90天。
    * 目前的monthly数据中取出了vids、tag_name，是由于数据规模决定的，没办法更大了，如果更大的话经常运行失败。
    *
    * @param spark 目前运行的spark_session
    * @param guid_vtags_path daily的数据目录，取多少天的，由tools控制
    * @param output_path 线上数据暂定为GuidVtagsMonthly
    */
  def get_guid_vtags_monthly(spark: SparkSession, guid_vtags_path: String, output_path: String): Unit = {
    import spark.implicits._

    // tag_tagid_weight 格式为： tag:String tagid:Long weight: Double


    val subpaths = Tools.get_last_month_date_str()
      .map(date=>(date, guid_vtags_path + "/" + date))
      .filter(kv => {Tools.is_path_exist(spark, kv._2) && Tools.is_flag_exist(spark, kv._2)})

    val today = Tools.get_n_day(0)
    if(subpaths.length == 0) {
      println("error, we have no vid data here!")
      return
    } else {
      println(f"we have ${subpaths.length}%d days guid vtags data to process.")
    }

    var guid_data_total: DataFrame = null
    for(guid_path_kv <- subpaths) {
      val diff_day = Tools.diff_date(guid_path_kv._1, today)
      val time_weight = if(diff_day == 0) 1 else Math.pow(diff_day.toDouble, -0.35)

      val time_weight_res = {
        if (time_weight > 1) {
          println(s"warning!!, time_weight upper than 1! value:$time_weight")
          1
        } else {
          time_weight
        }
      }

      // daily的数据为 "guid", "vids", "tagid_weight", "tagid_weight_normalize"  删掉最后一个
      val guid_data_daily = spark.read.parquet(guid_path_kv._2).select($"guid", $"tagid", ($"weight" * time_weight_res) as "weight")

      if(guid_data_total == null) {
        guid_data_total = guid_data_daily
      } else {
        guid_data_total = guid_data_total.union(guid_data_daily).toDF("guid", "tagid", "weight")
      }
    }

    val guid_data_total_res = guid_data_total.groupBy($"guid", $"tagid").agg(sum($"weight") as "weight").repartition(REPARTITION_NUM, $"guid")
    val vid_data_res = guid_data_total_res
      .groupBy($"guid").agg(collect_list(tuple2($"tagid", $"weight")) as "tagid_weight")
      .map(line=> {
        val guid = line.getString(0)
        val tagid_weight = line.getSeq[Row](1).map(r => (r.getLong(0), r.getDouble(1)))
          .sortBy(_._2)(Ordering[Double].reverse)
          .take(30)
          .map(r => (r._1, Tools.normalize(r._2)))
        (guid, tagid_weight)
      }).toDF("guid", "tagid_weight")

    vid_data_res.write.mode("overwrite").parquet(output_path)

  }

  /**
    * 把数据放到redis中去，有可能是先删再放
    *
    * @param path 这个位置的数据必须满足 get_guid_vtags_monthly()中的风格，也就是string || Seq[Long, Double]的风格
    * @param flag flag中如果有delete就先删除掉redis中的数据；如果有put则将数据放入redis中
    *
    */

  def put_guid_tag_to_redis(spark: SparkSession, path : String, flag: ArrayBuffer[String]): Unit = {
    println("put guid_tag to redis")
    import spark.implicits._
    val ip = "100.107.18.31"   //sz1159.show_video_hb_online.redis.com
    val port = 9039
    //val limit_num = 1000
    val bzid = "uc"
    val prefix = "G1"
    val tag_type: Int = 2501
    val data = spark.read.parquet(path)
      .map(line => {
        val guid = line.getString(0)
        val value_weight = line.getAs[Seq[Row]](1)
          .map(v => (v.getLong(0).toString, v.getDouble(1))).sortBy(_._2)(Ordering[Double].reverse).take(30).distinct
          .map(tp => (tp._1, tp._2))
        //_1 tag _2 tag_id _3 weight
        KeyValueWeight(guid, value_weight)
      })
      .cache
    //data.collect().foreach(line=>println(line.key))
    //println("\n\n\n\n")
    //data.filter(d => Tools.boss_guid.contains(d.key)).show()
    val test_redis_pool = new TestRedisPool(ip, port, 40000)
    val broadcast_redis_pool = spark.sparkContext.broadcast(test_redis_pool)
    if(flag.contains("delete")) {
      Tools.delete_to_redis(data, broadcast_redis_pool, bzid, prefix, tag_type /*, limit_num = 1000 */)
      println(s"redis delete done, number: ${data.count}")
    }

    if(flag.contains("put")) {
      Tools.put_to_redis(data, broadcast_redis_pool, bzid, prefix, tag_type /*, limit_num = 1000 */)
      println(s"put to redis done, number: ${data.count}")
    }
    Tools.stat(spark, data, "U1") // 统计数据
  }

  def main(args: Array[String]) {


    /**
      * step1. create SparkSession object
      * 封装了spark sql的执行环境，是spark SQL程序的唯一入口
      */
    //System.setProperty("hadoop.home.dir", "C:\\winutils")

    val spark = SparkSession
      .builder
      .appName(this.getClass.getName.split("\\$").last)
      .getOrCreate()


    val guid_input_path = args(0)
    val vid_sv_input_path = args(1)
    val output_path_daily_total = args(2)
    val date = args(3)
    val monthly_output_path = args(4)  // GuidVtagsMonthly

    val control_flag = args(5).split(Defines.FLAGS_SPLIT_STR, -1).toSet
    println("------------------[begin]-----------------")
    println("control flags is: " + control_flag.mkString("""||"""))

    if(control_flag.contains("daily")) {
      println("we will get guid vid data [daily]")
      guid_vid_sv_join_daily(spark, guid_input_path, vid_sv_input_path, output_path = output_path_daily_total + "/" + date)
    }

    if(control_flag.contains("monthly")) {
      println("we will get guid vid data [monthly]")
      get_guid_vtags_monthly(spark, guid_vtags_path = output_path_daily_total, output_path = monthly_output_path)
    }

    val flag: ArrayBuffer[String] = new ArrayBuffer[String]()
    if(control_flag.contains("delete")) {
      flag.append( "delete")
    }
    if(control_flag.contains("put")) {
      flag.append("put")
    }

    if(flag.nonEmpty) {
      put_guid_tag_to_redis(spark, path = monthly_output_path, flag = flag)
    } else {
      println("there's no control flags of redis put/delete, skip.")
    }
    println("------------------[done]-----------------")
  }

}
