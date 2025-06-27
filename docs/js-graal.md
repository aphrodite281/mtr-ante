# GraalJS 引擎更新​

自 ​ANTE 1.1.1​ 版本起，框架采用 ​GraalJS​ 引擎替代原有的 Rhino 引擎。此项变更带来以下改进与注意事项：

## ​现代化语法与性能​​
GraalJS 提供了对现代 JavaScript (ES) 语法的支持。  
性能差异可能不是很明显，因为当前强制使用解释执行模式。  
  
## 执行模式：
为确保兼容性，当前 ​ANTE 强制 GraalJS 运行于解释执行模式，暂时未启用 JIT 等运行时优化。  
未来版本将评估启用 JIT 优化的可能性。

## ​多线程支持​​
默认情况下 GraalJS 不支持多线程上下文共享，​ANTE 通过 Mixin 移除了此限制，允许脚本环境中进行多线程操作。
此修改理论上可能引入并发问题，但目前尚未在框架使用中观察到相关异常。

## 不同的处理机制
GraalJS 引擎与 Rhino 引擎面对Java参数的转换规则有些不同，以前的一些被忽略的类型转换可能在 GraalJS 中引发错误。
如无法将 (number) 1 传入 需要 String 的函数。

## 兼容性与导入​
为保持向后兼容，GraalJS 在 ANTE 中仍然支持传统的 importPackage、importClass 及 Packages 等导入方式。
​推荐在新项目中优先使用 GraalJS/JSR 223 标准方式 Java.type('fully.qualified.ClassName') 来引用 Java 类，这更符合现代最佳实践。