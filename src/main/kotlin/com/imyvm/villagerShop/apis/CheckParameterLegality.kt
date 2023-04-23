package com.imyvm.villagerShop.apis

fun checkParameterLegality(args: String): Pair<Int,MutableList<Items>> {
    val argList: List<String> = args.split(" ")
    val itemList = mutableListOf<Items>()
    val itemR = Regex("^[minecraft]+:[a-z_]+")
    val countR = Regex("^[0-9]+:+[0-9]+")
    var countItem = 0
    var countPrice = 0
    var compare = -1
    var item = ""
    for (i in argList) {
        if (itemR.matches(i)){
            countItem += 1
            item = i
        }else if (countR.matches(i)){
            countPrice += 1
            val (count,price) = i.split(":").map(String::toInt)
            itemList.add(Items(item,count,price))
        }
    }
    if (countItem == 0){
        compare = 0
    }else if (countItem != countPrice){
        compare = 1
    }
    return Pair(compare,itemList)
}