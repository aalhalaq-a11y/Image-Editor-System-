package model;

import java.awt.*;                       // للوصول للألوان، الرسومات، الخطوط
import java.awt.geom.AffineTransform;    // لتحويلات التدوير، التكبير، الانعكاس
import java.awt.image.*;                 // للتعامل مع BufferedImage و Raster
import java.io.*;                        // للتعامل مع الملفات
import javax.imageio.ImageIO;            // لقراءة وكتابة الصور بصيغ مختلفة

public class ImageProcessor {

    // ==================== المتغيرات ====================
    private BufferedImage currentImage;      // الصورة الحالية بعد التعديلات
    private BufferedImage originalImage;     // نسخة أصلية للاحتفاظ بها للـ Reset

    private int brightnessValue = 0;         // قيمة السطوع الحالية
    private int rotationAngle = 0;           // زاوية الدوران الحالية
    private int zoomLevel = 100;             // مستوى التكبير/التصغير

    private String format = "";              // امتداد الصورة (jpg, png ...)
    private long fileSize = 0;               // حجم الملف بالبايت

    // ==================== LOAD IMAGE ====================
    public void getloadImage(String filePath) throws Exception {

        File file = new File(filePath);                // إنشاء كائن ملف من المسار

        originalImage = ImageIO.read(file);           // قراءة الصورة الأصلية
        currentImage = deepCopy(originalImage);      // إنشاء نسخة عميقة للتعديل عليها

        format = getFileExtension(filePath);         // استخراج امتداد الصورة
        fileSize = file.length();                     // الحصول على حجم الملف

        // إعادة تهيئة القيم الافتراضية
        brightnessValue = 0;
        rotationAngle = 0;
        zoomLevel = 100;
    }

    // ==================== SAVE IMAGE ====================
    public void getsaveImage(String filePath, String format) throws Exception {
        if (currentImage == null) return;             // إذا لم تكن هناك صورة، لا نفعل شيء
        ImageIO.write(currentImage, format, new File(filePath));  // حفظ الصورة بالصيغ المطلوبة
    }

    // ==================== PROJECT SAVE ====================
    public void getsaveProject(String path) throws Exception {
        ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(path));
        out.writeObject(currentImage);               // حفظ الصورة ككائن في ملف
        out.close();
    }

    // ==================== PROJECT LOAD ====================
    public void getloadProject(String path) throws Exception {
        ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(path));
        currentImage = (BufferedImage) in.readObject(); // قراءة الصورة من ملف المشروع
        originalImage = deepCopy(currentImage);        // الاحتفاظ بنسخة أصلية
        in.close();
    }

    // ==================== GETTERS / SETTERS ====================
    public BufferedImage getCurrentImage() { return currentImage; }
    public BufferedImage getOriginalImage() { return originalImage; }

    public void setCurrentImage(BufferedImage img) { currentImage = deepCopy(img); }

    public int getZoomLevel() { return zoomLevel; }
    public void setZoomLevel(int zoomLevel) { this.zoomLevel = zoomLevel; }

    // ==================== GRAYSCALE ====================
    public void getapplyGrayscale() {
        if (currentImage == null) return;
        for (int y = 0; y < currentImage.getHeight(); y++) {
            for (int x = 0; x < currentImage.getWidth(); x++) {
                int rgb = currentImage.getRGB(x, y);

                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;

                int gray = (r + g + b) / 3;            // حساب التدرج الرمادي
                int newRGB = (gray << 16) | (gray << 8) | gray;  // تعيين RGB جديد
                currentImage.setRGB(x, y, newRGB);
            }
        }
    }

    // ==================== SEPIA ====================
    public void getapplySepia() {
        if (currentImage == null) return;
        for (int y = 0; y < currentImage.getHeight(); y++) {
            for (int x = 0; x < currentImage.getWidth(); x++) {
                int rgb = currentImage.getRGB(x, y);

                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;

                // تحويل RGB إلى Sepia
                int tr = getclamp((int)(0.393*r + 0.769*g + 0.189*b));
                int tg = getclamp((int)(0.349*r + 0.686*g + 0.168*b));
                int tb = getclamp((int)(0.272*r + 0.534*g + 0.131*b));

                currentImage.setRGB(x,y,(tr<<16)|(tg<<8)|tb);
            }
        }
    }

    // ==================== BRIGHTNESS ====================
    public void getadjustBrightness(int value) {
        if (currentImage == null) return;
        brightnessValue = value;

        for (int y=0;y<currentImage.getHeight();y++){
            for(int x=0;x<currentImage.getWidth();x++){
                int rgb = currentImage.getRGB(x,y);
                int r = getclamp(((rgb>>16)&255)+value);
                int g = getclamp(((rgb>>8)&255)+value);
                int b = getclamp((rgb&255)+value);
                currentImage.setRGB(x,y,(r<<16)|(g<<8)|b);
            }
        }
    }

    // ==================== CONTRAST ====================
    public void getadjustContrast(int value){
        if(currentImage==null) return;

        float factor = (259f*(value+255))/(255*(259-value));

        for(int y=0;y<currentImage.getHeight();y++){
            for(int x=0;x<currentImage.getWidth();x++){
                int rgb = currentImage.getRGB(x,y);

                int r = getclamp((int)(factor*(((rgb>>16)&255)-128)+128));
                int g = getclamp((int)(factor*(((rgb>>8)&255)-128)+128));
                int b = getclamp((int)(factor*((rgb&255)-128)+128));

                currentImage.setRGB(x,y,(r<<16)|(g<<8)|b);
            }
        }
    }

    // ==================== SATURATION ====================
    public void getadjustSaturation(int value){
        if(currentImage==null) return;

        float factor = 1 + value/100f;

        for(int y=0;y<currentImage.getHeight();y++){
            for(int x=0;x<currentImage.getWidth();x++){
                Color c = new Color(currentImage.getRGB(x,y));
                float[] hsb = Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(), null);
                hsb[1] = Math.min(1f, hsb[1]*factor);       // تعديل التشبع
                int rgb = Color.HSBtoRGB(hsb[0],hsb[1],hsb[2]);
                currentImage.setRGB(x,y,rgb);
            }
        }
    }

    // ==================== BLUR ====================
    public void getapplyBlur(){
        float[] kernel={ 1f/9,1f/9,1f/9, 1f/9,1f/9,1f/9, 1f/9,1f/9,1f/9 };
        ConvolveOp op = new ConvolveOp(new Kernel(3,3,kernel));
        currentImage = op.filter(currentImage,null);
    }

    // ==================== SHARPEN ====================
    public void getapplySharpen(){
        float[] kernel={ 0,-1,0, -1,5,-1, 0,-1,0 };
        ConvolveOp op = new ConvolveOp(new Kernel(3,3,kernel));
        currentImage = op.filter(currentImage,null);
    }

    // ==================== EDGE ====================
    public void getapplyEdgeDetection(){
        float[] kernel={ -1,-1,-1, -1,8,-1, -1,-1,-1 };
        ConvolveOp op = new ConvolveOp(new Kernel(3,3,kernel));
        currentImage = op.filter(currentImage,null);
    }

    // ==================== ROTATE ====================
    public void getrotateImage(int angle){
        if(currentImage==null) return;
        rotationAngle += angle;

        double radians = Math.toRadians(angle);
        int w = currentImage.getWidth();
        int h = currentImage.getHeight();

        BufferedImage rotated = new BufferedImage(w,h,currentImage.getType());
        Graphics2D g = rotated.createGraphics();
        AffineTransform at = AffineTransform.getRotateInstance(radians,w/2.0,h/2.0);
        g.drawImage(currentImage,at,null);
        g.dispose();
        currentImage = rotated;
    }

    // ==================== FLIP HORIZONTAL ====================
    public void getflipHorizontal(){
        if(currentImage==null) return;
        AffineTransform tx = AffineTransform.getScaleInstance(-1,1);
        tx.translate(-currentImage.getWidth(),0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        currentImage = op.filter(currentImage,null);
    }

    // ==================== FLIP VERTICAL ====================
    public void getflipVertical(){
        if(currentImage==null) return;
        AffineTransform tx = AffineTransform.getScaleInstance(1,-1);
        tx.translate(0,-currentImage.getHeight());
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        currentImage = op.filter(currentImage,null);
    }

    // ==================== RESIZE ====================
    public void getresizeImage(int width,int height){ getresizeImage(width,height,false); }

    public void getresizeImage(int width,int height,boolean keepRatio){
        if(currentImage==null) return;
        if(keepRatio){
            double ratio = (double)currentImage.getWidth()/currentImage.getHeight();
            height = (int)(width/ratio);
        }
        Image tmp = currentImage.getScaledInstance(width,height,Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width,height,currentImage.getType());
        Graphics2D g=resized.createGraphics();
        g.drawImage(tmp,0,0,null);
        g.dispose();
        currentImage=resized;
    }

    // ==================== CROP ====================
    public void getcropImage(int x, int y, int w, int h) {
        BufferedImage img = currentImage;
        if (x<0) x=0;
        if (y<0) y=0;
        if (x+w>img.getWidth()) w=img.getWidth()-x;
        if (y+h>img.getHeight()) h=img.getHeight()-y;
        currentImage = img.getSubimage(x,y,w,h);
    }

    // ==================== RESET ====================
    public void getresetImage(){
        if(originalImage!=null)
            currentImage=deepCopy(originalImage);
    }

    // ==================== HISTOGRAM ====================
    public int[] getHistogram(){
        int[] histogram=new int[256];
        if(currentImage==null) return histogram;
        for(int y=0;y<currentImage.getHeight();y++){
            for(int x=0;x<currentImage.getWidth();x++){
                int rgb=currentImage.getRGB(x,y);
                int gray=(((rgb>>16)&255)+((rgb>>8)&255)+(rgb&255))/3;
                histogram[gray]++;
            }
        }
        return histogram;
    }

    // ==================== METADATA ====================
    public String getMetadata(){
        if(currentImage==null) return "No Image Loaded";
        return "Width: "+currentImage.getWidth()+
                "\nHeight: "+currentImage.getHeight()+
                "\nFormat: "+format+
                "\nFile Size: "+(fileSize/1024)+" KB"+
                "\nZoom: "+zoomLevel+"%"+
                "\nBrightness: "+brightnessValue+
                "\nRotation: "+rotationAngle;
    }

    // ==================== HELPERS ====================
    private int getclamp(int v){ return Math.max(0,Math.min(255,v)); } // لتقييد الألوان بين 0-255

    private BufferedImage deepCopy(BufferedImage bi){
        ColorModel cm = bi.getColorModel();
        boolean alpha = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm,raster,alpha,null);
    }

    // ==================== WATERMARK ====================
    public void getaddWatermark(String text){
        BufferedImage img = currentImage;
        Graphics2D g2 = img.createGraphics();
        g2.setFont(new Font("Arial",Font.BOLD,40));
        g2.setColor(new Color(255,255,255,120));  // لون أبيض شفاف
        int x = img.getWidth()/4;
        int y = img.getHeight()/2;
        g2.drawString(text,x,y);
        g2.dispose();
        currentImage = img;
    }

    // ==================== GET FILE EXTENSION ====================
    private String getFileExtension(String name){
        int i=name.lastIndexOf('.');
        if(i>0) return name.substring(i+1).toLowerCase();
        return "";
    }

}