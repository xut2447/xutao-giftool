package com.xutao.giftool.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.xutao.giftool.GifImageUtil;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

@SpringJUnitWebConfig
@SpringBootTest
public class GifImageUtilTest {

    /**
     * @Author tao.xu
     * @Description GIF文件压缩测试
     * @Date 15:47 2021-04-15
     **/
    @Test
    public void compressTest() {
        var imageSrcPath = ResourceUtil.getResource("test.gif").getPath();
        var gifsicleUtil = GifImageUtil.instance();
        var img_data = FileUtil.readBytes(imageSrcPath);
        var file_data = gifsicleUtil.compress(img_data, 540, null, null);
        System.out.println(img_data.length);
        System.out.println(file_data.length);
        Assertions.assertTrue(file_data.length > 0);
    }

    /**
     * @Author tao.xu
     * @Description 获取GIF图片信息
     * @Date 15:47 2021-04-15
     **/
    @Test
    public void getImageInfoTest(){
        var imageSrcPath = ResourceUtil.getResource("test.gif").getPath();
        var gifsicleUtil = GifImageUtil.instance();
        var img_data = FileUtil.readBytes(imageSrcPath);
        var file_data = gifsicleUtil.getImageInfo(img_data);
        System.out.println(file_data);
        Assertions.assertTrue(file_data.getFramesCount() > 0);
    }

    /**
     * @Author tao.xu
     * @Description 获取GIF首帧图片
     * @Date 15:47 2021-04-15
     **/
    @Test
    public void getFirstFrameTest(){
        var imageSrcPath = ResourceUtil.getResource("test.gif").getPath();
        var gifsicleUtil = GifImageUtil.instance();
        var img_data = FileUtil.readBytes(imageSrcPath);
        var file_data = gifsicleUtil.getFirstFrame(img_data, 540, null);
        Assertions.assertTrue(img_data.length > 100);
    }
}
