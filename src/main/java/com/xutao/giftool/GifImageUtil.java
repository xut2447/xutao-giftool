package com.xutao.giftool;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.*;

import java.io.*;
import java.net.URLConnection;

/**
 * @Author tao.xu
 * @Description GIF图片工具类
 *              使用方式：var img_data = FileUtil.readBytes('输入文件.gif');
 *                      var gifsicleUtil = GifsicleUtil.instance();
 *                      var file_data = gifsicleUtil.Compress(img_data, 540, null, null);
 * @Date 18:13 2021-04-14
 * @return return
 **/
public class GifImageUtil {

    @Getter
    @Setter
    private String commandDir;   // 执行目录

    @Getter
    private final String gifsicleTmpDir = new File("").getCanonicalPath() + "/gifsicle_tmp/";  // 临时目录

    @Getter
    private final String gifsicleTmpCacheDir = new File("").getCanonicalPath() + "/gifsicle_tmp/data_cache/";  // 临时缓存目录

    // 图片信息实体类
    @Data
    @Builder
    public static class ImageInfo{
        // 帧数
        private Integer framesCount;
        // 宽度
        private Integer imageWidth;
        // 高度
        private Integer imageHeight;
        // 色值
        private Integer imageColors;
        // 大小
        private Integer fileSize;
    }

    /**
     * @Author tao.xu
     * @Description 构造函数
     * @Date 18:33 2021-04-14
     **/
    public GifImageUtil() throws IOException {
        init();
    }

    /**
     * @return return 工具实例
     * @Author tao.xu
     * @Description 初始化工具类
     * @Date 18:34 2021-04-14
     **/
    @SneakyThrows
    public static GifImageUtil instance(){
        return new GifImageUtil();
    }

    /**
     * @return return
     * @Author tao.xu
     * @Description 初始化
     * @Date 18:19 2021-04-14
     **/
    @Synchronized
    private void init() throws IOException {
        String osName   = getOsName();  // 获取系统名称
        String gifsiclePath = "gifsicle/" + osName;  // 执行文件所在目录

        var gifsicle_TmpDir = new File(gifsicleTmpDir);  // 临时目录
        var gifsicle_TmpCacheDir = new File(gifsicleTmpCacheDir);  // 缓存目录

        // 创建临时目录
        if(!gifsicle_TmpDir.exists()){
            var mkdirs_result = gifsicle_TmpDir.mkdirs();
            if(!mkdirs_result){
                throw new RuntimeException("创建文件夹失败！" + gifsicleTmpDir);
            }
        }

        // 创建缓存目录
        if(!gifsicle_TmpCacheDir.exists()){
            var mkdirs_result = gifsicle_TmpCacheDir.mkdirs();
            if(!mkdirs_result){
                throw new RuntimeException("创建文件夹失败！" + gifsicleTmpCacheDir);
            }
        }

        // 将可执行文件复制到临时文件夹中
        this.commandDir = gifsicle_TmpDir.getPath();
        String extension = getExtensionByOs(osName);
        InputStream gifsicleFileStream    = GifImageUtil.class.getResourceAsStream("/" + gifsiclePath + "/gifsicle" + extension);

        var copyExecfileToPath = new File(gifsicle_TmpDir.getPath() + "/gifsicle" + extension);
        if(!copyExecfileToPath.exists()){
            getFile(gifsicleFileStream , copyExecfileToPath.getPath());
        }
    }

    // 复制文件
    private void getFile(InputStream is,String fileName) throws IOException {
        BufferedInputStream in=null;
        BufferedOutputStream out=null;
        in=new BufferedInputStream(is);
        out=new BufferedOutputStream(new FileOutputStream(fileName));
        int len=-1;
        byte[] b=new byte[1024];
        while((len=in.read(b))!=-1){
            out.write(b,0,len);
        }
        in.close();
        out.close();
    }

    /**
     * @param imgData 源图片流，必填
     * @param resizeWidth 裁剪宽度，当图片宽度小于该值时，则采用图片原来的宽度 ，可选
     * @param resizeHeight 裁剪高度，当图片宽度小于该值时，则采用图片原来的宽度，如果不填，则自动按照比例缩放高度 , 可选
     * @return return 返回压缩后的图片字节流
     * @Author tao.xu
     * @Description GIF图片压缩
     * @Date 18:36 2021-04-14
     **/
    public byte[] compress(byte[] imgData,Integer resizeWidth, Integer resizeHeight, Integer colors) {
        if(imgData == null || imgData.length <= 0){
            throw new RuntimeException("参数imgData不能为空");
        }

        if(!imgIsGif(imgData)){
            throw new RuntimeException("只支持GIF文件");
        }

        // 参数处理
        if(colors == null || colors > 256 || colors < 2){  // GIF色值，可选值在2-256之间
            colors = 256;
        }

        // 创建缓存文件
        var cache_file_name = cacheImage(imgData);

        // 将图片放到缓存目录
        try {

            var cmd_resize_width = "";  // 裁剪宽度参数
            var cmd_resize_height = ""; // 裁剪高度参数
            if (resizeWidth != null && resizeWidth > 0) {
                cmd_resize_width = StrUtil.format("--resize-width={}", resizeWidth.toString());
            }
            if (resizeHeight != null && resizeHeight > 0) {
                cmd_resize_height = StrUtil.format("--resize-height={}", resizeHeight.toString());
            }

            // 组装命令行
            var cmd = StrUtil.format(
                    "{}/gifsicle -O3 -o {} {} --colors={} {} {}",
                    this.commandDir,
                    cache_file_name,
                    cache_file_name,
                    colors.toString(),
                    cmd_resize_width,
                    cmd_resize_height
            );

            // 执行命令
            var cmd_result = executeCommand(cmd);
            System.out.println(cmd_result);
            return FileUtil.readBytes(cache_file_name); // 返回文件流
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }finally {
            FileUtil.del(cache_file_name);  // 删除文件
        }
    }

    /**
     * @param imgData 源图片流，必填
     * @param resizeWidth 裁剪宽度，当图片宽度小于该值时，则采用图片原来的宽度 ，可选
     * @param resizeHeight 裁剪高度，当图片宽度小于该值时，则采用图片原来的宽度，如果不填，则自动按照比例缩放高度 , 可选
     * @return return
     * @Author tao.xu
     * @Description 提取第一帧
     * @Date 10:34 2021-04-15
     **/
    public byte[] getFirstFrame(byte[] imgData, Integer resizeWidth, Integer resizeHeight){
        //  命令格式：gifsicle 111.gif "#0" -o 222.png --resize=100x100

        if(imgData == null || imgData.length <= 0){
            throw new RuntimeException("参数imgData不能为空");
        }

        if(!imgIsGif(imgData)){
            throw new RuntimeException("只支持GIF文件");
        }

        // 创建缓存文件
        var cache_file_name = cacheImage(imgData);
        var result_png = cache_file_name.toLowerCase().replace(".gif", ".png");  // 输出PNG
        try{

            var cmd_resize_width = "";  // 裁剪宽度参数
            var cmd_resize_height = ""; // 裁剪高度参数
            if (resizeWidth != null && resizeWidth > 0) {
                cmd_resize_width = StrUtil.format("--resize-width={}", resizeWidth.toString());
            }
            if (resizeHeight != null && resizeHeight > 0) {
                cmd_resize_height = StrUtil.format("--resize-height={}", resizeHeight.toString());
            }

            // 组装命令行
            var cmd = StrUtil.format(
                    "{}/gifsicle {} \"#0\" -o {} {} {}",
                    this.commandDir,
                    cache_file_name,
                    result_png,
                    cmd_resize_width,
                    cmd_resize_height
            );

            // 执行命令
            var cmd_result = executeCommand(cmd);
            System.out.println(result_png);
            return FileUtil.readBytes(result_png); // 返回文件流

        }catch (Exception ex){
            System.out.println("获取GIF帧失败");
            throw new RuntimeException(ex);
        }finally {
            FileUtil.del(cache_file_name);  // 删除文件
            FileUtil.del(result_png);  // 删除文件
        }
    }

    /**
     * @param imgData 图片内容
     * @return return
     * @Author tao.xu
     * @Description 获取图片信息
     * @Date 10:57 2021-04-15
     **/
    public ImageInfo getImageInfo(byte[] imgData){
        if(imgData == null || imgData.length <= 0){
            throw new RuntimeException("参数imgData不能为空");
        }

        if(!imgIsGif(imgData)){
            throw new RuntimeException("只支持GIF文件");
        }

        // 创建缓存文件
        var cache_file_name = cacheImage(imgData);
        try{
            var exec_img_info = executeCommand(StrUtil.format("{}/gifsicle -I {}", this.commandDir, cache_file_name));

            // 接续 & 组装结果
            var frames_count = Convert.toInt(ReUtil.findAllGroup0("\\.gif.[^\\n]*\\n", exec_img_info).get(0).split(" ")[1].trim());
            var img_size = ReUtil.findAllGroup0("logical screen.[^\\n]*\\n", exec_img_info).get(0).split(" ")[2].trim();
            var img_size_width = Convert.toInt(img_size.split("x")[0].trim());
            var img_size_height = Convert.toInt(img_size.split("x")[1].trim());
            var img_colors = Convert.toInt(ReUtil.findAllGroup0("global color table.[^\n]*\n", exec_img_info).get(0).replace("global color table ", "").replace("[", "").replace("]", "").trim());

            return ImageInfo.builder()
                    .imageColors(img_colors)
                    .framesCount(frames_count)
                    .imageHeight(img_size_height)
                    .imageWidth(img_size_width)
                    .fileSize(imgData.length)
                    .build();
        }catch (Exception ex){
            System.out.println("获取GIF信息失败");
            throw new RuntimeException(ex);
        }finally {
            FileUtil.del(cache_file_name);  // 删除文件
        }
    }

    /**
     * @param imgData 图片字节流
     * @return return true or false
     * @Author tao.xu
     * @Description 检测是否是GIF图片
     * @Date 10:25 2021-04-15
     **/
    private Boolean imgIsGif(byte[] imgData){
        var file_type =getImageType(imgData);
        return !StrUtil.isBlank(file_type) && file_type.equals(".gif");
    }

    /**
     * @param imgData 图片字节流
     * @return return 缓存路径
     * @Author tao.xu
     * @Description 将文件缓存到临时文件夹
     * @Date 10:24 2021-04-15
     **/
    private String cacheImage(byte[] imgData){

        // 创建缓存文件
        var cache_file_name = this.getGifsicleTmpCacheDir() + RandomUtil.randomString(32) + ".gif";
        FileUtil.writeBytes(imgData, cache_file_name);
        return cache_file_name;
    }

    /**
     * @param imgdata 图片字节流
     * @return return gif
     * @Author tao.xu
     * @Description 从图片流中获取扩展名
     * @Date 19:58 2021-04-14
     **/
    @SneakyThrows
    private String getImageType(byte[] imgdata){
        InputStream is = new ByteArrayInputStream(imgdata);
        String mimeType = URLConnection.guessContentTypeFromStream(is);
        mimeType = mimeType.replace("image/", ".");  //返回的是image/gif
        return mimeType;
    }

    /**
     * @param command 命令
     * @return return
     * @Author tao.xu
     * @Description 执行命令
     * @Date 18:32 2021-04-14
     **/
    @SneakyThrows
    private String executeCommand(String command) {
        System.out.println("Execute: " + command);

        var tv_result = new StringBuilder();
        Process p = Runtime.getRuntime().exec(command);
        InputStream is = p.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            tv_result.append(line).append("\n");
        }
        p.waitFor();
        is.close();
        reader.close();
        p.destroy();
        return tv_result.toString();
    }

    /**
     * @return return .exe(windows), ""(linux)
     * @Author tao.xu
     * @Description 获取执行文件后缀名
     * @Date 18:24 2021-04-14
     **/
    private String getExtensionByOs(String os) {
        if (os == null || os.isEmpty()) return "";
        else if (os.contains("win")) return ".exe";
        return "";
    }

    /**
     * @return return
     * @Author tao.xu
     * @Description 获取系统名称，用于判断使用哪个执行包，linux_64 or windows_x64，目前只支持x64系统
     * @Date 18:14 2021-04-14
     **/
    private String getOsName() {
        var OS_NAME = SystemUtil.getOsInfo().getName().toLowerCase();  // 获取系统名称

        // windows
        if (OS_NAME.contains("win")) {
            return "windows_x64";
        } else if (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.indexOf("aix") > 0) {
            // unix
            return "linux_x64";
        } else {
            throw new RuntimeException("oops!!该工具类不支持当前系统！");
        }
    }

}
