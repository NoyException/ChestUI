# 管理员手册

## 使用Json自定义CUI

在服务器的`plugins`下找到目录`plugins/ChestUI/cui`（如果没有，请新建目录），在其中放入你的json文件。

文件名没有任何影响，请确保其为`.json`结尾。

详细语法描述待完成，您可以先参考一份样例：

<details>
  <summary>点我展开样例</summary>

```json
{
  "key": "test:ui1",
  "singleton": true,
  "title": "测试",
  "maxRow": 6,
  "maxColumn": 9,
  "layers": {
    "1": {
      "maxRow": 1,
      "maxColumn": 9,
      "marginTop": 0,
      "marginLeft": 0,
      "relative": false,
      "slots": {
        "(0, 0)": {
          "type": "button",
          "material": "RED_STAINED_GLASS_PANE",
          "displayName": "退出",
          "onClicks": {
            "LEFT": [
              {
                "action": "command-op",
                "value": "cui close @s top"
              }
            ]
          }
        },
        "(0, 1)": {
          "type": "button",
          "material": "BLACK_STAINED_GLASS_PANE",
          "displayName": ""
        },
        "(0, 2)": {
          "type": "button",
          "material": "BLACK_STAINED_GLASS_PANE",
          "displayName": ""
        },
        "(0, 3)": {
          "type": "button",
          "material": "BLACK_STAINED_GLASS_PANE",
          "displayName": ""
        },
        "(0, 4)": {
          "type": "button",
          "material": "BLACK_STAINED_GLASS_PANE",
          "displayName": ""
        },
        "(0, 5)": {
          "type": "button",
          "material": "BLACK_STAINED_GLASS_PANE",
          "displayName": ""
        },
        "(0, 6)": {
          "type": "button",
          "material": "BLACK_STAINED_GLASS_PANE",
          "displayName": ""
        },
        "(0, 7)": {
          "type": "button",
          "material": "BLACK_STAINED_GLASS_PANE",
          "displayName": ""
        },
        "(0, 8)": {
          "type": "button",
          "material": "BLACK_STAINED_GLASS_PANE",
          "displayName": ""
        }
      }
    }
  }
}
```

</details>

## CUI指令

使用如下指令查看帮助：
```minecraft
/cui help
```

## 预置CUI

### CUI监控

用于监控当前所有存在的CUI。

```minecraft
/cui monitor
```

打开后，可以看见当前所有正在运行的CUI。点击CUI即可将其打开。

### 背包监控

用于监控所有线上玩家的背包。

```minecraft
/cui create chestui:im keepAlive
/cui open chestui:im#1
```

打开后，可以看见所有在线玩家的背包。点击玩家头颅即可查看其背包。

## 自定义CUI

TODO