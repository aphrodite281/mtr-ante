# 自定义配置

## ConfigResponder

ANTE 使用 `ConfigResponder` 接口来表示配置的响应器，存储配置的信息。更进一步的，请查看[源代码-ConfigResponder.java](https://github.com/aphrodite281/mtr-ante/blob/alpha/common/src/main/java/cn/zbx1425/mtrsteamloco/data/ConfigResponder.java)
`ConfigResponder` 需要实现以下方法：

- `ConfigResponder.key(): String`

    获取配置项的标识。

- `ConfigResponder.init(Map<String, String> configMap): void`

    初始化配置项。(在每次注册的时候调用)

- `ConfigResponder.getListEntries(Map<String, String> configMap, ConfigEntryBuilder builder, Supplier<Screen> screenSupplier): List<AbstractConfigListEntry>`

    获取列表条目。`Supplier` 位于 `java.util.function` 包中，这里您可以调用 `screenSupplier.get()` 来获取这个界面。您可以写一些逻辑在点击时跳转到另一个界面，在关闭时返回到当前界面。

### TextField

`ConfigResponder` 提供了一个基础的实现: `TextField`


- `new TextField(key: String, name: Component, defaultValue: String)`

- `new TextField(key: String, name: Component, defaultValue: String, transformer: Function<String, String>, errorSupplier: Function<String, Optional<Component>>, saveConsumer: Consumer<String>, tooltipSupplier: Function<String, Optional<Component[]>>, requireRestart: boolean)`

    创建一个配置响应器。

包含以下属性以及对应方法：

| 属性 | 方法 | 说明 |
| ------------- | ------------- | ------------- |
| `final TextField.key: String` | 无 | 配置项的标识。无法修改 |
| `TextField.name: Component` | `TextField.setName(name: Component): TextField` | 配置项的名称 |
| `TextField.defaultValue: String` | `TextField.setDefaultValue(defaultValue: String): TextField` | 配置项的默认值 |
| `TextField.transformer: Function<String, String>` | `TextField.setTransformer(transformer: Function<String, String>): TextField` | 配置项的转换器 |
| `TextField.errorSupplier: Function<String, Optional<Component>>` | `TextField.setErrorSupplier(errorSupplier: Function<String, Optional<Component>>): TextField` | 配置项的错误提示 |
| `TextField.saveConsumer: Consumer<String>` | `TextField.setSaveConsumer(saveConsumer: Consumer<String>): TextField` | 配置项的保存函数 |
| `TextField.tooltipSupplier: Function<String, Optional<Component[]>>` | `TextField.setTooltipSupplier(tooltipSupplier: Function<String, Optional<List<Component>>>): TextField` | 配置项的提示信息 |
| `TextField.requireRestart: boolean` | `TextField.setRequireRestart(requireRestart: boolean): TextField` | 配置项是否需要重启游戏 |

上文中的 [Optional](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) 是 Java 8 引入的类，用来表示一个值可能为空，防止空指针异常。ANTE 已将此添加到了 JavaScript 环境中，您可以直接使用。

`tooltipSupplier` 变量的返回值应该是 `Optional<Component[]>` 类型，您需要用 `asJavaArray([...])` 方法将JS的数组转换为Java的数组。

最后，本类支持链式调用。



## ClientConfig

为了让资源包可以在游戏中调整设置，ANTE向Js环境提供了 `ClientConfig` 类，可以注册和获取配置，配置文件会被保存在客户端。注册后的配置将会出现在 由 `/mtrnte config` 命令调出的设置界面的最下方。更进一步的，请查看[源代码-ClientConfig.java](https://github.com/aphrodite281/mtr-ante/blob/alpha/common/src/main/java/cn/zbx1425/mtrsteamloco/ClientConfig.java)

- `static ClientConfig.register(response: TextField): void `

    添加一个响应器。

- `static get(key: String): String`

    `String` key: 配置项的标识。

- `static save(): void`

    保存所有配置项到本地文件。

`ClientConfig`

## 示例

### 使用长构造函数

```javascript
const configKey = "myConfig";
// ···
// 如果值不是 true 或 false，则提示用户输入错误
const errorSupplier = (str) => {
    if (str == "true" || str == "false") return java.util.Optional.empty();
    else return java.util.Optional.of(ComponentUtil.translatable("text.aph.config.error"));
}

const res = new ConfigResponder.TextField(configKey, 
    ComponentUtil.translatable("text.aph.config.myConfig"), "true", 
    value => value, errorSupplier, str => {}, 
    str => java.util.Optional.empty(), false);

ClientConfig.register(res);

function render(ctx, state, entity) {
    // ···
    let config = ClientConfig.get(configKey);// String
    ctx.setDebugInfo("myConfig: " + config)
    // ···
}
// ···
```

### 使用链式调用

```javascript
const configKey = "myConfig";
const errorSupplier = (str) => {
    if (str == "true" || str == "false") return java.util.Optional.empty();
    else return java.util.Optional.of(ComponentUtil.translatable("text.aph.config.error"));
}

const res = new ConfigResponder.TextField(configKey, 
    ComponentUtil.translatable("text.aph.config.myConfig"), "true")
    .setErrorSupplier(errorSupplier);
    .setTooltipSupplier(str => java.util.Optional.of(asJavaArray([ComponentUtil.translatable("text.aph.config.tooltip")])));

ClientConfig.register(res);
```

### 包装一个配置项

```javascript

function newBooleanToggleResponder(key, name, defaultValue) {
    return new ConfigResponder({
        key: () => key,
        init: (configMap) => {
            if (!configMap.containsKey(key)) configMap.put(key, defaultValue + "");
        },
        getListEntries: (configMap, builder, screenSupplier) => {
            let value = configMap.get(key);
            let flag = false;
            if (value + "" == "true") flag = true;
            return [builder.startBooleanToggle(name, flag)
                .setDefaultValue(defaultValue)
                .setSaveConsumer((value) => {
                    configMap.put(key, value + "");
                }).build()];
        }
    })
}

let res = newBooleanToggleResponder("myConfig", ComponentUtil.translatable("text.aph.config.myConfig"), true);

ClientConfig.register(res);

```