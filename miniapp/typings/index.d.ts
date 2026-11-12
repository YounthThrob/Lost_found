// 极简微信小程序全局声明，保证 TS 编译通过。
// 真实类型建议在微信开发者工具中安装 miniprogram-api-typings，会生成完整 typings。

declare const wx: any;
declare function App(options: any): void;
declare function Page(options: any): void;
declare function Component(options: any): void;
declare function getApp<T = any>(): T;
declare function getCurrentPages(): any[];

// 小程序运行时提供的全局定时器与日志。
declare function setTimeout(cb: (...args: any[]) => void, ms?: number): number;
declare function clearTimeout(id: number): void;
declare function setInterval(cb: (...args: any[]) => void, ms?: number): number;
declare function clearInterval(id: number): void;
declare const console: any;
