package com.ahandyapp.airnavx.model

data class CraftSpec(
//    var type: String,
    var typeInx: Int = 0,
    var typeList: ArrayList<String> = arrayListOf("C172", "PA28", "PA34"),

    var craftDimsC172: CraftDims = CraftDims(craftType = "C172", wingspan = 36.0, length = 27.17),
    var craftDimsPA28: CraftDims = CraftDims(craftType = "PA28", wingspan = 28.22, length = 21.72),
    var craftDimsPA34: CraftDims = CraftDims(craftType = "PA34", wingspan = 38.9, length = 27.58),

    var dimsList: ArrayList<CraftDims> = arrayListOf(craftDimsC172, craftDimsPA28, craftDimsPA34),

    var craftTagC172List: java.util.ArrayList<String> = arrayListOf("UNKNOWN", "N2621Z", "N20283", "New(type)", "New(speak)"),
    var craftTagPA28List: java.util.ArrayList<String> = arrayListOf("UNKNOWN", "N21803", "N38657", "New(type)", "New(speak)"),
    var craftTagPA34List: java.util.ArrayList<String> = arrayListOf("UNKNOWN", "N142GD", "N12345", "New(type)", "New(speak)"),

    var tagInx: Int = 0,
    var tagList: ArrayList<ArrayList<String>> = arrayListOf(craftTagC172List, craftTagPA28List, craftTagPA34List)

)
