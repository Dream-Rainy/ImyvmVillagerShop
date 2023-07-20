# Imyvm Villager Shop

***

[![Lisense](https://img.shields.io/github/license/Dream-Rainy/ImyvmVillagerShop?style=for-the-badge)](https://github.com/Dream-Rainy/ImyvmVillagerShop/blob/1.19/LICENSE)![Build](https://img.shields.io/github/actions/workflow/status/Dream-Rainy/ImyvmVillagerShop/build.yml?style=for-the-badge)

**注意，该库仍在开发状态中，目前仅完成了部分指令的注册以及数据的存取，如有任何想法欢迎提issue，有能力者可以帮忙完善并提出pr。**
## 一、配置文件
文件位置：`config/imyvm_villagershop.conf`

```yaml
core {
  # 指定显示语言
  language="en_us"
  tax {
    # 玩家补货时的税率
    restock=0.01
  }
  # 数据库相关设置
  database {
      # 数据库类型，支持 "POSTGRESQL" , "MYSQL" , "ORACLE" 以及 "SQLSERVER"
      # 只有 POSTGRESQL 确认可用，其余理论可行
      database=POSTGRESQL
      # 数据库地址，默认为 POSTGRESQL
      # mysql相关地址 ： jdbc:mysql://localhost:3306/imyvmvillagershop
      # 或 Oracle : jdbc:oracle:thin:@//localhost:1521/imyvmvillagershop
      # 或 SQL Server : jdbc:sqlserver://localhost:32768;databaseName=imyvmvillagershop
      # 如果均不可用，插件会在world文件夹下自动生成Sqlite数据库文件
      url="jdbc:pgsql://localhost:5432/imyvmvillagershop"
      # 数据库用户名
      username=root
      # 数据库密码
      password="1145141919810"
      # 仅在MySQL数据库生效，用来调整Hikari池的大小
      maximumPoolSize=10
  }
}
```

管理员可以使用`/villagershop config reload`来重载配置，使用`/villagershop taxrate set <taxrate>`来设置税率。

## 二、商店创建

### 1.玩家商店

玩家可以使用指令`/villagershop create shop <shopname> <x y z> <item> <count> <price>`[^1]创建商店，并扣除一定的货币。  
货币的花费会随着玩家拥有商店数量的增加而增加，函数表达式为:

```math
f(x)
\begin{cases}
40, &x\ \le\ 3\\
10\times2^{x-1}, &x\ \gt\ 3
\end{cases}
```

**注意**

- 商店刚创建或商品刚添加时默认会使用背包内所有已有物品进行免费补货，你也在创建完成后额外使用`/villagershop item addstock <shopname> <item> [count]`来对商店中**存在的商品**进行“补货”，`count`是一个可选参数，若不填则默认消耗背包内所有特定物品。  
同时，补货需要消耗一定的货币，计算公式如下:

```math
f(x) = p \times c \times t
```

> ```math
> p为设置价格，c为补货数量，t为税率，默认为0.1\%
> ```

- 同一玩家创建的商店**不允许**名称相同，当个商店的商品**数量上限为7个物品**，价格下限**不低于**服务器设置的物价或系统商店物价。
- 创建商店时的`count`参数代表用`price`的钱可以买到`count`个数的物品  
举个栗子，比如我使用`/villagershop create shop DreamRain 0 0 0 minecraft:cherry_leaves 2 100`创建了一个名为`DreamRain`并出售`樱花树叶`的商店。那么，我可以使用`100`货币买到`2`个`樱花树叶`。而不是商店中`樱花树叶`的库存为`2`。
- 所有商店都有一个唯一的`shopnumber`标识符，并且永远不可能重复。

### 2.系统商店

管理员可以使用指令`/villagershop create adminshop <shopname> <x y z> <item1> <count>:<price> <item2> <count>:<price> ... <item_n> <count>:<price>`来创建系统商店。  
系统商店库存**无限**，但是可以使用玩家商店中的`stock`参数来限制单个玩家的购买数量。  

## 三、商店管理

### 1.货物设置

在商店创建成功后，玩家可以使用`/villagershop item add <shopname> <item> <count> <price>`来添加商品，使用`/villagershop item change <shopname> <item> <count> <price>`来修改**已有**商品的价格以及出售数量，使用`/villagershop item delete <shopname> <item>`[^1]来删除已有的商品，该操作会返还该商品的所有库存。  

### 2.商店设置

玩家可以使用`/villagershop manager info set {shopname,id} {shopnamenew,x y z}`来修改商店的名称或者位置。当你不想要该商店时，可以使用`/villagershop delete {shopname,shopnumber}`[^1]来删除该商店，管理员可以使用`shopnumber`删除任意不符合规定的商店，无需提前通知拥有者。删除商店会返还所有的库存，但是并不会返还创建时消耗的货币。同时，被管理员删除的商店并不会返还库存  
同时，管理员可以使用`/villagershop manager search {shopname:shopname, location:location, owner:owner, range:range}`，owner参数可以使用玩家**创建商店时**的ID。可以使用`/villagershop manager inquire <shopnumber>`获取特定商店的详细信息。    
管理员在征得玩家以及管理组同意的情况下，可以使用`/villagershop setAdmin <shopnumber>`[^1]来将该商店转化为系统商店。**注意，该操作也不会返还库存。**  

## 四、收购相关

目前收购改为实体商店收购，仍在实现中。  
收购价格为系统商店在售价格的**80%**  

## 五、其他

### TODO：  
 - [x] 商店创建或物品添加时即消耗背包内的物品补货  
 - [x] 删除商店或商品时返还库存  
 - [ ] 补货时可以从玩家身上的潜影盒中获取所需物品  
 - [ ] 系统商店限制单个玩家购买数量  
 - [x] 支持范围搜索商店，以及通过玩家名称搜索商店，不输出重复结果。  
 - [ ] 完成商店的实体创建  
 - [ ] 完成GUI呈现以及操作监听  

[^1]: 对于该指令，你需要使用`/villagershop confirm`来确定执行。同时，你可以使用`/villagershop cancel`来取消操作，所有操作在60s后都会自动取消。并且，在执行下一个需要确认的操作前，你需要先确认已有的操作。
