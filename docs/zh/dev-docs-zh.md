# 开发者手册

## 添加依赖

注意把下列的 `version` 替换为最新的版本号。

### Maven:

pom.xml:
```xml
<dependency>
  <groupId>cn.noy</groupId>
  <artifactId>chest-ui</artifactId>
  <version>version</version>
</dependency>
```

### Gradle:

build.gradle:
```groovy
dependencies {
  implementation 'cn.noy:chest-ui:version'
}
```

build.gradle.kts:
```kotlin
dependencies {
  implementation("cn.noy:chest-ui:version")
}
```

## 自定义ChestUI

### 实现CUIHandler

自定义CUI需要实现 `CUIHandler<T>` 接口，其中 `T` 为你的CUI的类型，例如：

```java
public class TestCUI implements CUIHandler<TestCUI> {
    private ChestUI<TestCUI> cui;
	@Override
	public void onInitialize(ChestUI<TestCUI> cui) {
        // 初始化CUI
        this.cui = cui.edit()
                .doSomething()
                .doSomething()
                .finish();
    }
}
```

你可以为你的CUI添加一些注解，来更方便地初始化：

- @CUI: 定义CUI的id，这样就能在游戏内通过/cui操作该CUI
- @DefaultCamera: 初始化默认Camera（默认是3*9的大小）
- @CUITitle: 初始化标题
- @CUISize: 初始化CUI大小，默认无限大

### 插入图层

```java
@CUI("test")
public class TestCUI implements CUIHandler<TestCUI> {
    private ChestUI<TestCUI> cui;
    @Override
    public void onInitialize(ChestUI<TestCUI> cui) {
        cui.edit().setLayer(0, new Layer(1, 9).edit()
                .editAll((slotHandler, row, column) -> slotHandler.button(
                        builder -> builder.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build()))
                .editSlot(0, 0, slotHandler -> slotHandler.button(builder -> builder
                        .material(Material.RED_STAINED_GLASS_PANE).displayName("Previous").clickHandler(event -> {
                            // 点击事件
                        }).build()))
                .editSlot(0, 8, slotHandler -> slotHandler.button(builder -> builder
                        .material(Material.GREEN_STAINED_GLASS_PANE).displayName("Next").clickHandler(event -> {
                            // 点击事件
                        }).build()))
                .finish()).finish();
    }
}
```

上面的操作在深度为0的地方设置了一个图层，这个图层的大小是1*9，里面的除了首位所有格子都是黑色玻璃片。第一列和第九列分别设置了一个红色和绿色的玻璃片。你可以为红绿色玻璃片设置点击事件。

深度越小，图层越靠前。在同一个深度中，后设置的图层会覆盖先设置的图层。深度不能小于0。

### 槽位类型

目前支持四种槽位类型：

- empty: 空槽位
- button: 按钮，可以设置点击事件
- filter: 滤镜，可以对更深图层中的槽位进行修饰
- storage: 存储，可以用于存放物品。你可以自定义存储源（比如玩家背包、合成台格子）

### 参考

你可以参考prebuilt包中的CUI来学习如何自定义CUI。

## 打开ChestUI

你需要使用 `ChestUIManager` 来创建一个CUI：

```java
// 先获取本plugin实例
var cuiPlugin = JavaPlugin.getPlugin(CUIPlugin.class);
var cui = cuiPlugin.getCUIManager().createCUI(TestCUI.class);
```

接下来，你可以选择获取默认摄像头，或者创建一个摄像头

```java
var camera1 = cui.getDefaultCamera();
var camera2 = cui.createCamera(); // 克隆了一个默认摄像头
var camera3 = cui.createCamera(...); // 创建一个摄像头
```

最后，通过摄像头来打开CUI

```java
camera1.open(player, false); // false表示不记录玩家当前打开的摄像头，关闭本摄像头时不会跳转到上一个摄像头
```

在摄像头中，对某个深度插入`Layer`，会覆盖`ChestCUI`中对应深度的`Layer`。此外，虽然CUI中的深度不能小于0，但是摄像头可以。

## 自定义合成

### 配方

一个配方由数个Consumer和Producer组成。Consumer消耗物品，Producer生成物品。

你可以定义自己的Consumer和Producer来实现更高程度的自定义配方。

#### Consumer

目前提供了两种Consumer：
- ShapedConsumer: 定义一个固定的形状，按照固定摆放形式消耗物品（这是MC原版最常见的配方模式）
- ShapelessConsumer: 定义一个无序的消耗物品列表，消耗物品的摆放顺序并不重要

在创建Consumer时，你需要提供`Ingredient`。你可以自定义`Ingredient`，但我们也为您提供了几种常用的`Ingredient`：
- ExactIngredient: 精确地匹配物品
- MaterialMatchedIngredient: 匹配物品的类型
- MetaMatchedIngredient: 匹配物品的meta
- BucketIngredient: 匹配牛奶桶、水桶等物品，消耗完后会剩下一个桶

#### Producer

目前提供了两种Producer：
- ShapedProducer: 定义一个固定的形状，按照固定摆放形式生成物品
- ShapelessProducer: 定义一个没有固定形式的生成物品列表，生成的物品将会按顺序排列

在创建Producer时，你需要提供`Product`。你可以自定义`Product`，但我们也为您提供了几种常用的`Product`：
- ExactProduct: 生成一个精确的物品
- RandomProduct: 按照给定的权重生成一个随机的物品。如果配方没有成功使用，生成的结果将会保留，防止刷随机

#### 创建配方

比如，一个打火石的配方：

```java
var recipe = Recipe.builder()
        .strict(true)
        .addConsumer(ShapelessConsumer.builder()
                .strict(true)
                .add(new ExactIngredient(Material.IRON_INGOT))
                .add(new ExactIngredient(Material.FLINT))
                .build())
        .addProducer(ShapelessProducer.builder()
                .add(new ExactProduct(Material.FLINT_AND_STEEL))
                .build())
        .build();
```

一个蛋糕的配方：
```java
var recipe = Recipe.builder()
        .strict(true)
        .addConsumer(ShapedConsumer.builder()
                .strict(true)
                .pattern("MMM", "SES", "WWW")
                .set('M', new BucketIngredient(Material.MILK_BUCKET))
                .set('S', new ExactIngredient(Material.SUGAR))
                .set('E', new ExactIngredient(Material.EGG))
                .set('W', new ExactIngredient(Material.WHEAT))
                .build())
        .addProducer(ShapelessProducer.builder()
                .add(new ExactProduct(Material.CAKE))
                .build())
        .build();
```

### 合成台

下面的代码自定义了一个合成台

```java
var craftingTable = CraftingTable.builder()
        .mode(CraftingTable.Mode.AUTO)
        .addInput(5, 5)
        .addOutput(3, 3)
        .ioType(CraftingTable.IOType.CAMERA)
        .addRecipe(recipe1)
        .addRecipe(recipe2)
        .build();
```

你可以为合成台定义多个input（比如铁砧的界面就有两个），但同时你的配方也要添加两个`Consumer`。output同理。

接下来，你可以使用以下代码来将合成台显示到CUI：

```java
var input0 = craftingTable.generateInputLayer(0, camera);
var input1 = craftingTable.generateInputLayer(1, camera);
var output = craftingTable.generateInputLayer(0, camera);
cui.edit()
        .setLayer(0, input0)
        .setLayer(1, input1)
        .setLayer(2, output)
        .finish();
```

如果你给合成台设置的io类型模式是`SHARED`，那么上面的`camera`可以替换为`null`。

## 事件监听

TODO