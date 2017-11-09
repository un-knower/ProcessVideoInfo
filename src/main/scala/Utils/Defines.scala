package Utils

/**
  * Created by baronfeng on 2017/9/15.
  */
object Defines {
  // tag停止词表
  val STOP_WORDS_PATH: String = "/user/baronfeng/useful_dataset/stop_words.txt"

  val TAG_HASH_LENGTH: Int = Math.pow(2, 24).toInt // 目前暂定2^24为feature空间

  // 剧名的分解 获取cid/vid的干净剧名
  val pattern_list = Array(
    """.*(一|二|三|四|五|六|七|八|九)$""".r,
    """(第(一|二|三|四|五|六|七|八|九|十|\d)(集|季|部|期))|(season \d+)|(\d+)$""".r,
    """(预告|电影|大电影|电影大全|系列|视频|电视剧|电视连续剧|电视剧大全|节目|电视节目)$""".r,
    """(合集|全集|电影合集|电影全集|电视剧全集)$""".r,
    """(版|中文|中文版|英文|英语|英文版|国语|国语版|2D版|3D版|3d版|2d|2D|3d|3D)$""".r,
    """(枪版|高清|高清版|DVD版|蓝光|蓝光版)""".r,
    """(\s)""".r,
    """[——！，。·～？、~@#￥%……&*（）：；《）《》“”\(\)»〔〕\-「」]\+""".r,
    """[<> _,()~\';\":-`!@#$%^&*+=\[\]{}>?/.\|\\\n\r\t]""".r
  )

  // Hanlp 分词时候，有用的词性
  val useful_set = Map(
    "a" -> "形容词",
    "ad" -> "副形词",
    "ag" -> "形容词性语素",
    "al" -> "形容词性惯用语",
    "an" -> "名形词",

    "b" -> "区别词",
    "begin" -> "仅用于始##始",
    "bg" -> "区别语素",
    "bl" -> "区别词性惯用语",

    //      "c" -> "连词",
    //      "cc" -> "并列连词",
    //
    //      "d" -> "副词",
    //      "dg" -> "辄,俱,复之类的副词",
    //      "dl" -> "连语",

//    "e" -> "叹词",
    "end" -> "仅用于终##终",

    "f" -> "方位词",

    "g" -> "学术词汇",
    "gb" -> "生物相关词汇",
    "gbc" -> "生物类别",
    "gc" -> "化学相关词汇",
    "gg" -> "地理地质相关词汇",
    "gi" -> "计算机相关词汇",
    "gm" -> "数学相关词汇",
    "gp" -> "物理相关词汇",

    "h" -> "前缀",
    "i" -> "成语",
    "j" -> "简称略语",
    "k" -> "后缀",
    "l" -> "习用语",

    //      "m" -> "数词",
    //      "mg" -> "数语素",
    //      "Mg" -> "甲乙丙丁之类的数词",
    //      "mq" -> "数量词",

    "n" -> "名词",
    "nb" -> "生物名",
    "nba" -> "动物名",
    "nbc" -> "动物纲目",
    "nbp" -> "植物名",
    "nf" -> "食品，比如“薯片”",
    "ng" -> "名词性语素",
    "nh" -> "医药疾病等健康相关名词",
    "nhd" -> "疾病",
    "nhm" -> "药品",
    "ni" -> "机构相关（不是独立机构名）",
    "nic" -> "下属机构",
    "nis" -> "机构后缀",
    "nit" -> "教育相关机构",
    "nl" -> "名词性惯用语",
    "nm" -> "物品名",
    "nmc" -> "化学品名",
    "nn" -> "工作相关名词",
    "nnd" -> "职业",
    "nnt" -> "职务职称",
    "nr" -> "人名",
    "nr1" -> "复姓",
    "nr2" -> "蒙古姓名",
    "nrf" -> "音译人名",
    "nrj" -> "日语人名",
    "ns" -> "地名",
    "nsf" -> "音译地名",
    "nt" -> "机构团体名",
    "ntc" -> "公司名",
    "ntcb" -> "银行",
    "ntcf" -> "工厂",
    "ntch" -> "酒店宾馆",
    "nth" -> "医院",
    "nto" -> "政府机构",
    "nts" -> "中小学",
    "ntu" -> "大学",
    "nx" -> "字母专名",
    "nz" -> "其他专名",

    //      "r" -> "代词",
    //      "rg" -> "代词性语素",
    //      "Rg" -> "古汉语代词性语素",
    //      "rr" -> "人称代词",
    //      "ry" -> "疑问代词",
    //      "rys" -> "处所疑问代词",
    //      "ryt" -> "时间疑问代词",
    //      "ryv" -> "谓词性疑问代词",
    //      "rz" -> "指示代词",
    //      "rzs" -> "处所指示代词",
    //      "rzt" -> "时间指示代词",
    //      "rzv" -> "谓词性指示代词",
    //      "s" -> "处所词",
    //      "t" -> "时间词",
    //      "tg" -> "时间词性语素",

    //      "v" -> "动词",
    //      "vd" -> "副动词",
    //      "vf" -> "趋向动词",
    //      "vg" -> "动词性语素",
    "vi" -> "不及物动词（内动词）",
    "vl" -> "动词性惯用语",
    "vn" -> "名动词"

  )

  val FLAGS_SPLIT_STR = raw"---"

}
