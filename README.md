# MusicPlayer
## 描述
这是一个安卓系统的本地音乐播放器。通过查询安卓的`MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`的声音媒体库，获得本地的音乐文件引用。

创建了前台服务用于播放音乐，前台服务使得播放过程不会被系统强制关闭。

## 功能
1. 播放/暂停音乐
2. 音乐进度条
3. 上一首、下一首
4. 专辑图片获取及不停旋转
5. 歌曲名和歌手的分割显示
6. 支持顺序、随机、单曲循环播放

## 使用的安卓特性
1. databinding
2. MVVM(ViewModel)
3. Lifecycle(LiveData)
4. Navigation(Fragment)
