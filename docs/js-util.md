# JavaScript 工具类

NTE 提供了一些工具类，以便获取一些信息或更简单地实现功能。



## 输出测试信息

- `static print(params: Object...): void`

调用这个函数会在 Minecraft 日志里打出信息（在游戏内没有信息显示）。可以传入任意多个任意类型的参数。



## 版本

提供了一些能用来获得版本号的函数，以便让作者能兼容不同版本的不同（如果有）。

| 函数                                         | 说明                                                         |
| -------------------------------------------- | ------------------------------------------------------------ |
| `static Resources.getMTRVersion(): String`   | MTR 的版本字符串，形如 `1.19.2-3.1.0-hotfix-1`               |
| `static Resources.getNTEVersion(): String`   | NTE 的版本字符串，形如 `0.4.0+1.19.2`                        |
| `static Resources.getNTEVersionInt(): int`   | NTE 的版本的数字形式，以便比较；例如 0.4.0 的是 4000，1.9.1 的会是 19100 |
| `static Resources.getNTEProtoVersion(): int` | NTE 的存档格式版本数字。                                     |



## TextUtil

MTR 采用了一个 `中文部分|English Part||EXTRA` 的车站命名方法，所以 NTE 提供了一些函数来把各个部分拆出来。

| 成员                                                         | 说明                           |
| ------------------------------------------------------------ | ------------------------------ |
| `static TextUtil.getCjkParts(src: String): String`           | 获取里面的中文部分。           |
| `static TextUtil.getNonCjkParts(src: String): String`        | 获取里面的英文部分。           |
| `static TextUtil.getExtraParts(src: String): String`         | 获取里面的隐藏部分。           |
| `static TextUtil.getNonExtraParts(src: String): String`      | 获取里面的中文和英文部分。     |
| `static TextUtil.getNonCjkAndExtraParts(src: String): String` | 获取里面的英文和隐藏部分。     |
| `static TextUtil.isCjk(src: String): String`                 | 检查一个字符串是否包含中文字。 |



## Timing

- `static Timing.elapsed(): double`

  游戏的运行时间。单位是秒，是递增的，游戏暂停时会停止增长。

- `static Timing.delta(): double`

  本次调用 `render` 和上次之间的时间差。可以用来计算例如轮子这段时间里应该转过的角度等。



## StateTracker

有时候需要计测状态的转换。例如，只在通过某个位置时播放一次广播（因为如果 `if (...distance < 300) ctx.play...` 的话就会在通过之后每帧都满足条件然后每帧都播放一次，造成几百个广播百花齐放的效果），或者在切换页面后前一秒钟里显示动画。

因为每列车都该有独立的逻辑，您大概会想把它存进列车的 `state` 里。

- `new StateTracker()`

  创建一个 StateTracker。

- `StateTracker.setState(value: String): void`

  告诉它目前的状态是这个状态。不调用的时候则是继续保持状态。

- `StateTracker.stateNow(): String`

  获取目前的状态。

- `StateTracker.stateLast(): String`

  获取上一个状态。没有的话，返回 `null`。

- `StateTracker.stateNowDuration(): double`

  获取目前的状态已经持续了的时间。

- `StateTracker.stateNowFirst(): boolean`

  是否是刚刚通过 `setState` 换进了这个状态。



## CycleTracker

这是一个按时间自动循环切换的 `StateTracker`。

因为每列车都该有独立的逻辑，您大概会想把它存进列车的 `state` 里。

- `new CycleTracker(params: Object[])`

  创建一个 CycleTracker。params 是它会循环切换的各个状态和每个状态应该持续的时间，单位是秒。
  例如 `new CycleTracker(["route", 5, "nextStation", 5])`

- `CycleTracker.tick(): void`

  根据现在的时间来更新状态。

- `CycleTracker.stateNow(): String`

  获取目前的状态。

- `CycleTracker.stateLast(): String`

  获取上一个状态。没有的话，返回 `null`。

- `CycleTracker.stateNowDuration(): double`

  获取目前的状态已经持续了的时间。

- `CycleTracker.stateNowFirst(): boolean`

  是否是刚刚通过 `tick` 换进了这个状态。



## RateLimit

有些工作不需要太频繁地进行，例如显示屏可能只需要每秒更新 10 次而不是每帧都更新。所以可以限制它们的频率来提升性能。

因为每列车都该有独立的逻辑，您大概会想把它存进列车的 `state` 里。

- `new RateLimit(interval: double)`

  创建一个 RateLimit。interval 是两次直接应该有的间隔，单位是秒，例如 interval 为 0.1 就代表应该一秒十次。

- `RateLimit.shouldUpdate(): boolean`

  距离上次运行之间是否已经经过了足够的时间。将所需的代码用 `if (state.rateLimitXXX.shouldUpdate()) { ... }` 包起来即可限制它的频率。

- `RateLimit.resetCoolDown(): void`

  重置时间，让它马上就可以再次运行。



## MTRClientData

MTR 的客户端数据，可以用来读取换乘线路等。参见 MTR 源码 ClientData.java。



## MinecraftClient

由于混淆表原因，没办法直接把客户端的类搬出来让您使用。所以这里有一些辅助方法。

- `static MinecraftClient.worldIsRaining(): boolean`

  世界是否在下雨。

- `static MinecraftClient.worldIsRainingAt(pos: Vector3f): boolean`

  世界的某个方块处是否正在下雨而且被淋到。

- `static MinecraftClient.worldDayTime(): int`

  世界的一天内时间，单位是 Tick。

- `static MinecraftClient.narrate(text: String): void`

  调用系统“讲述人”读出一段文本。

- `static MinecraftClient.displayMessage(message: String,actionBar :boolean): void`

  在聊天框或在操作栏（物品栏上方）显示一段文本。当 `actionBar` 为 `true` 时，显示在操作栏，否则显示在聊天框。



## TickableSound

这个类提供了一个可以实时更新参数的声音方法。

- `new TickableSound(sound :ResourceLocation)`

  以 BLOCK 声源 创建一个 `TickableSound`。

- `new TickableSound(sound :ResourceLocation, source :SoundSource)`

  以指定声源创建 `TickableSound`。

- `TickableSound.setData(volume :float, pitch :float, pos :Vector3f): void`

  设置声音的音量、音调、位置。

- `TickableSound.setLooping(looping :boolean): void`

  设置是否循环播放。

- `TickableSound.setDelay(delay :int): void`

  设置延迟播放的时间。

- `TickableSound.setAttenuation(attenuation :boolean): void`

  设置声音衰减。

- `TickableSound.setRelative(relative :boolean): void`

  设置声音是否相对于玩家。

- `TickableSound.play(): void`

  播放声音。(相当于 `SoundHelper.play(this)`)

- `TickableSound.quit(): void`

  停止播放声音。(相当于 `SoundHelper.stop(this)`)

- `TickableSound.pause(): void`

  暂停播放声音。(与上一个效果相近)

- `TickableSound.getSound(): Sound`

  获取声音。

- `TickableSound.getLocation(): ResourceLocation`

  获取声音的标签位置。

- `TickableSound.getSource(): SoundSource`

  获取声音的声源。

- `TickableSound.isLooping(): boolean`

  是否循环播放。

- `TickableSound.getVolume(): float`

  获取声音的音量。

- `TickableSound.getPitch(): float`

  获取声音的音调。

- `TickableSound.getDelay(): int`

  获取延迟播放的时间。

- `TickableSound。getAttenuation(): SoundInstance.Attenuation`

  获取声音衰减。

- `TickableSound.isRelative(): boolean`

  是否声音相对于玩家。

- `TickableSound.toString(): String`

  转为字符串（调试时自动调用）。



## SoundHelper

这个类提供了一些声音相关的功能。

- `static SoundHelper.play(sound :SoundInstance): void`

  播放一个声音实例，如 `TickableSound` 或 `TrainLoopingSoundInstance`

- `static SoundHelper.play(sound :SoundInstance, delay :int): void`

  延迟一段时间后播放声音。

- `static SoundHelper.play(sound :SoundEvent, pos :Vector3f,  volume :float, pitch :float): void`

  播放一个 `SoundEvent`, 类似 ctx.playSound......

- `static SoundHelper.play(sound :SoundEvent, pos :Vector3f, source :SoundSource, volume :float, pitch :float): void`

  以指定声源播播放一个 `SoundEvent。`

- `static SoundHelper.stop(): void`

  停止所有声音。

- `static SoundHelper.stop(sound :SoundInstance): void`

  停止某个声音实例。

- `static SoundHelper.stop(sound :SoundEvent, source :SoundSource): void`

  停止使用某个声源播放的某一个 `SoundEvent` 的所有声音。

- `static SoundHelper.stop(sound :SoundEvent): void`

 停止使用 BLOCK 声源播放的某一个 `SoundEvent` 的所有声音。

- `static SoundHelper.getSoundSource(str :String): SoundSource`

  根据字符串获取一个 `SoundSource`。

## ParticleHelper

这个类提供了一些粒子相关的功能。

- `static ParticleHelper.addParticle(particle :ParticleOptions, pos :Vector3f, config :Vector3f): void`

  在某个位置添加一个粒子。

- `static ParticleHelper.addParticle(particle :ParticleOptions, isOverrideLimiter :boolean, pos :Vector3f, config :Vector3f): void`

  在某个位置添加一个粒子，并忽略粒子限制器。

- `static ParticleHelper.addParticle(particle :ParticleOptions, isOverrideLimiter :boolean, isOnGround :boolean, pos :Vector3f, config :Vector3f): void`

  在某个位置添加一个粒子，并忽略粒子限制器和是否在地上。

- `static ParticleHelper.getParticleType(particleName :String): <T> SimpleParticleType`

  根据名字获取一个 `SimpleParticleType` 或 `ParticleType`。名称可以查看 [Minecraft Wiki](https://zh.minecraft.wiki/wiki/%E7%B2%92%E5%AD%90) 的 `JAVA版ID名` 一栏。

## ComponentUtil

为了方便创建我的世界(文本)组件，我向JS环境中添加了ComponentUtil类，含有以下方法：

- `static MutableComponent(Component) translatable(String text, Object... objects)`
  获取一个可翻译的组件，参数为文本和可变参数，返回一个可翻译的组件。

- `static MutableComponent(Component) literal(String text)`
将字符串转化为组件

- `static String getString(Component component)`
将组件转化为字符串

### 示例

例如在aph:lang文件夹中放入了 zh_cn.json 文件，内容如下：
```json
{
    "text.aph.qssnn": "晴纱是男娘",
    "text.aph.is": "%s是%s"
}
```

在aph:lang文件夹中放入了 en_us.json 文件，内容如下：
```json
{
    "text.aph.qssnn": "Qingsha is a male girl",
    "text.aph.is": "%s is %s"
}
```

```javascript
// 若为英语环境 则组件内容为 "Qingsha is a male girl"，若为中文环境 则组件内容为 "晴纱是男娘"。
const qssnn = ComponentUtil.translatable("text.aph.qssnn");

// 若为英语环境 则组件内容为 "A is B"，若为中文环境 则组件内容为 "A是B"。
const is = ComponentUtil.translatable("text.aph.is", "A", "B");

const text = "Hello, world!";
const component = ComponentUtil.literal(text);// 转换为 Component
const result = ComponentUtil.getString(component);// 转换为 String
```
