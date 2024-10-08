# Imyvm Villager Shop

***

[![Lisense](https://img.shields.io/github/license/Dream-Rainy/ImyvmVillagerShop?style=for-the-badge)](https://github.com/Dream-Rainy/ImyvmVillagerShop/blob/1.19/LICENSE)![Build](https://img.shields.io/github/actions/workflow/status/Dream-Rainy/ImyvmVillagerShop/build.yml?style=for-the-badge)

## 一、配置文件
文件位置：`config/imyvm_villagershop.conf`

```yaml
core {
  # 指定显示语言
  language="zh_cn"
  tax {
    # 玩家补货时的税率
    restock=0.01
  }
  # 数据库相关设置
  database {
      # 数据库类型，支持 "POSTGRESQL" , "MYSQL" , "H2"
      database=H2
      # 数据库地址，默认为 H2
      url="localhost:5432/imyvmvillagershop"
      # 数据库用户名
      username=root
      # 数据库密码
      password=""
      # 仅在MySQL数据库生效，用来调整Hikari池的大小
      maximumPoolSize=10
  }
}
```

管理员可以使用`/villagerShop config reload`来重载配置，使用`/villagerShop taxRate set <taxRate>`来设置税率。

## 二、商店创建

### 1.玩家商店

玩家可以使用指令`/villagerShop create shop <shopname> <x y z> <item> <quantitySoldEachTime> <price>`[^1]创建商店，并扣除一定的货币。  
货币的花费会随着玩家拥有商店数量的增加而增加，函数表达式为:

```math
f(x)
\begin{cases}
40, &x\ \le\ 3\\
10\times2^{x-1}, &x\ \gt\ 3
\end{cases}
```

**注意**

- 商店刚创建或商品刚添加时默认会使用背包内**所有**（无法选择是否添加）已有物品进行免费补货。
你也在创建完成后额外使用`/villagerShop item addStock <shopName> <item> [quantity]`来对商店中**存在的商品**进行“补货”，
`quantity`是一个可选参数，若不填则默认消耗背包内所有特定物品。  
同时，补货需要消耗一定的货币，计算公式如下:

```math
f(x) = p \times c \times t
```

> ```math
> p为设置价格，c为补货数量，t为税率，默认为0.1\%
> ```

- 同一玩家创建的商店**不允许**名称相同，当个商店的商品**数量上限为7个物品**，价格下限**不低于**服务器设置的物价或系统商店物价。
- 创建商店时的`quantitySoldEachTime`参数代表用`price`的钱可以买到`quantitySoldEachTime`个数的物品  
举个栗子，比如我使用
`/villagerShop create shop DreamRain 0 0 0 minecraft:cherry_leaves 2 100`
创建了一个名为`DreamRain`并出售`樱花树叶`的商店。
那么，我可以使用`100`货币买到`2`个`樱花树叶`。而不是商店中`樱花树叶`的库存为`2`。
- 所有商店都有一个唯一的`id`标识符，并且永远不可能重复。
- 目前商品支持保存NBT（未经充分测试），可能会导致无法获取到指定的物品，无法保留自定义NBT等问题。

### 2.系统商店

管理员可以使用指令
`/villagerShop create adminShop <shopName> <x y z> <type> <item1> <count>:<price>:[stock] <item2> <count>:<price>:[stock] ... <item_n> <count>:<price>:[stock]`
来创建系统商店。  
系统商店库存**无限**，但是可以使用玩家商店中的`stock`参数来限制单个玩家的购买数量。  
`stock`可以为空，如果为空，则默认为不限量。

## 三、商店管理

### 1.货物设置

在商店创建成功后，玩家可以使用
`/villagerShop item add <shopName> <item> <quantitySoldEachTime> <price>`
来添加商品，
使用
`/villagerShop item change <shopName> <item> <quantitySoldEachTime> <price>`
来修改**已有**商品的价格以及出售数量，
使用
`/villagerShop item delete <shopName> <item>`[^1]来删除已有的商品，
该操作会返还该商品的所有库存。  

### 2.商店设置

玩家可以使用
`/villagerShop manager changeInfo setShopName {shopNameOld, id} <shopNameNew>`
来修改商店的名称，
可以使用
`/villagerShop manager changeInfo setShopName {shopName, id} <x y z>`
来修改商店的位置。
当你不想要该商店时，可以使用
`/villagerShop delete {shopname, shopnumber}`[^1]
来删除该商店，管理员可以使用`id`删除任意不符合规定的商店，无需提前通知拥有者。
删除商店会返还所有的库存，但是并不会返还创建时消耗的货币。
同时，被管理员删除的商店并不会返还库存。  
同时，管理员可以使用
`/villagerShop manager search {shopname:shopname, location:location, owner:owner, range:range}`，
owner参数可以使用玩家**创建商店时**的ID。
可以使用`/villagerShop manager inquire <id>`获取特定商店的详细信息。    
管理员在征得玩家以及管理组同意的情况下，可以使用
`/villagerShop setAdmin <shopnumber>`[^1]
来将该商店转化为系统商店。**注意，该操作也不会返还库存。**  

## 四、收购相关

仅管理员可创建收购商店，在创建时将`<type>`参数设置为`UNLIMITED_BUY`即可创建**无限**收购指定物品的商店。  
或者将`<type>`参数设置为`REFRESHABLE_BUY`即可每日对限制进行更新。  
收购价格由使用者自行设定。  
收购也可以使用stock参数来设定每天每人可以出售给系统商店的数量。  
`stock`可以为空，如果为空，则默认为不限量。  

## 五、其他

### TODO：  
 - [x] 商店创建或物品添加时即消耗背包内的物品补货  
 - [x] 删除商店或商品时返还库存  
 - [ ] 补货时可以从玩家身上的潜影盒中获取所需物品  
 - [x] 系统商店限制单个玩家购买数量  
 - [x] 支持范围搜索商店，以及通过玩家名称搜索商店，不输出重复结果。  
 - [x] 完成商店的实体创建  
 - [x] 完成GUI呈现以及操作监听
 - [x] 支持保存物品NBT
 - [ ] 将计算公式独立进配置文件
 - [ ] 在<item>参数为空的情况下直接添加主手物品

[^1]: 对于该指令，你需要使用`/villagerShop confirm`来确定执行。同时，你可以使用`/villagerShop cancel`来取消操作，所有操作在60s后都会自动取消。并且，在执行下一个需要确认的操作前，你需要先确认已有的操作。