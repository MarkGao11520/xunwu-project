function load(city, regions, aggData) {
    // 搜索百度地图API功能

    // 创建实例，设置地区显示最大级别为城市
    var map = new BMap.Map("allmap",{minZoom:12});
    // 北京中心
    var point = new BMap.Point(116.403981,39.915046);
    // 初始化坐标，设置中心点和地图界别
    map.centerAndZoom(point,12);
    //添加比例尺空间
    map.addControl(new BMap.NavigationControl({enableGeolocation:true}));
    //添加滚轮
    map.addControl(new BMap.ScaleControl({anchor:BMAP_ANCHOR_TOP_LEFT}));
}