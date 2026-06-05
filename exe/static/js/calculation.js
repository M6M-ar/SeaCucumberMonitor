function rad(d) {
    return d * Math.PI / 180.0
}

function getDistance(lat1, lng1, lat2, lng2) {
    var radLat1 = rad(lat1);
    var radLat2 = rad(lat2);
    var a = radLat1 - radLat2;
    var b = rad(lng1) - rad(lng2);
    var s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
        Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
    s = s * 6378.137; // EARTH_RADIUS;
    s = Math.round(s * 10000) / 10000; //输出为公里

    var distance = s;
    var distance_str = "";

    if (parseInt(distance) >= 1) {
        distance_str = distance.toFixed(1) + "km";
    } else {
        distance_str = distance * 1000 + "m";
    }

    //s=s.toFixed(4);

    console.info('lyj 距离是', s);
    console.info('lyj 距离是', distance_str);
    return distance_str;
}

function getAngle(lat_a,lng_a, lat_b, lng_b) {
    var a = (90 - lat_b) * Math.PI / 180;
    var b = (90 - lat_a) * Math.PI / 180;
    var AOC_BOC = (lng_b - lng_a) * Math.PI / 180;
    var cosc = Math.cos(a) * Math.cos(b) + Math.sin(a) * Math.sin(b) * Math.cos(AOC_BOC);
    var sinc = Math.sqrt(1 - cosc * cosc);
    var sinA = Math.sin(a) * Math.sin(AOC_BOC) / sinc;
    var A = Math.asin(sinA) * 180 / Math.PI;
    var res = 0;
    if (lng_b > lng_a && lat_b > lat_a) res = A;
    else if (lng_b > lng_a && lat_b < lat_a) res = 180 - A;
    else if (lng_b < lng_a && lat_b < lat_a) res = 180 - A;
    else if (lng_b < lng_a && lat_b > lat_a) res = 360 + A;
    else if (lng_b > lng_a && lat_b == lat_a) res = 90;
    else if (lng_b < lng_a && lat_b == lat_a) res = 270;
    else if (lng_b == lng_a && lat_b > lat_a) res = 0;
    else if (lng_b == lng_a && lat_b < lat_a) res = 180;
    const y = Math.sin(lng_b-lng_a) * Math.cos(lat_b);
    const x = Math.cos(lat_a)*Math.sin(lat_b) -
        Math.sin(lat_a)*Math.cos(lat_b)*Math.cos(lng_b-lng_a);
    const θ = Math.atan2(y, x);
    const brng = (θ*180/Math.PI + 360) % 360; // in degrees
    console.log('角度666：'+brng)
    console.log('角度：'+res.toFixed(2))
    var angle=res.toFixed(2);
    if(isNaN(res)){
        angle=brng.toFixed(2)
    }
    return angle;
}