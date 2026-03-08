package controller;

// ====================== IMPORTS ======================
// استيراد الكلاسات اللازمة من المشروع وجافا
import model.*;
import view.MainFrame;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.io.File;

public class Controller {

    // ====================== ATTRIBUTES ======================
    // المتغيرات الأساسية للكلاس
    private final MainFrame view;          // واجهة المستخدم (GUI)
    private final ImageProcessor processor; // كلاس معالجة الصور (تطبيق الفلاتر والتعديلات)
    private final HistoryManager history;   // إدارة تاريخ التعديلات (Undo/Redo)

    // ====================== CONSTRUCTOR ======================
    public Controller(MainFrame view, ImageProcessor processor, HistoryManager history) {
        this.view = view;
        this.processor = processor;
        this.history = history;

        initListeners();   // تهيئة كل المستمعين (الأزرار والسلايدرز)
        initDragDrop();    // تهيئة السحب والإفلات للصور
    }

    // ====================== INIT LISTENERS ======================
    private void initListeners() {

        // ================= OPEN IMAGE =================
        view.getOpenBtn().addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            // فتح نافذة اختيار ملف
            if (chooser.showOpenDialog(view) == JFileChooser.APPROVE_OPTION)
                gethandleLoad(chooser.getSelectedFile().getAbsolutePath());
        });

        // ================= SAVE IMAGE =================
        view.getSaveBtn().addActionListener(e -> gethandleSaveDialog());

        // ================= FILTERS =================
        // ربط الأزرار بفلاتر الصور
        view.getGrayBtn().addActionListener(e -> gethandleGrayscale());
        view.getBlurBtn().addActionListener(e -> gethandleBlur());
        view.getSepiaBtn().addActionListener(e -> gethandleSepia());
        view.getSharpenBtn().addActionListener(e -> gethandleSharpen());
        view.getEdgeBtn().addActionListener(e -> gethandleEdge());

        // ================= HISTORY =================
        view.getUndoBtn().addActionListener(e -> gethandleUndo());
        view.getRedoBtn().addActionListener(e -> gethandleRedo());

        // ================= CROP =================
        view.getCropBtn().addActionListener(e -> gethandleCrop());

        // ================= IMAGE INFO =================
        view.getMetaBtn().addActionListener(e -> gethandleMetadata());
        view.getHistogramBtn().addActionListener(e -> gethandleHistogram());

        // ================= RESET =================
        view.getResetBtn().addActionListener(e -> gethandleReset());

        // ================= ZOOM =================
        view.getZoomInBtn().addActionListener(e ->
                gethandleZoom(processor.getZoomLevel() + 10)); // تكبير
        view.getZoomOutBtn().addActionListener(e ->
                gethandleZoom(processor.getZoomLevel() - 10)); // تصغير

        // ================= ROTATE =================
        view.getRotateLeftBtn().addActionListener(e -> gethandleRotate(-90)); // تدوير لليسار
        view.getRotateRightBtn().addActionListener(e -> gethandleRotate(90)); // تدوير لليمين
        view.getRotateCustomBtn().addActionListener(e -> gethandleCustomRotate()); // تدوير مخصص

        // ================= RESIZE =================
        view.getResizeBtn().addActionListener(e -> gethandleResize());

        // ================= SLIDERS =================
        // ربط السلايدرز بتعديل الصورة مباشرة
        view.getBrightnessSlider().addChangeListener(e -> {
            if (!view.getBrightnessSlider().getValueIsAdjusting())
                gethandleBrightness(view.getBrightnessSlider().getValue());
        });

        view.getContrastSlider().addChangeListener(e -> {
            if (!view.getContrastSlider().getValueIsAdjusting())
                gethandleContrast(view.getContrastSlider().getValue());
        });

        view.getSaturationSlider().addChangeListener(e -> {
            if (!view.getSaturationSlider().getValueIsAdjusting())
                gethandleSaturation(view.getSaturationSlider().getValue());
        });
    }

    // ====================== DRAG & DROP ======================
    private void initDragDrop() {
        // السماح بسحب وإفلات صورة على الواجهة مباشرة
        view.setDropFileListener(file -> gethandleLoad(file.getAbsolutePath()));
    }

    // ====================== HISTORY ======================
    private void updateHistoryCounter() {
        // تحديث عداد Undo / Redo على الواجهة
        view.getupdateHistoryCount(
                history.getUndoCount(),
                history.getRedoCount()
        );
    }

    // ====================== LOAD IMAGE ======================
    public void gethandleLoad(String path) {
        try {
            processor.getloadImage(path);           // تحميل الصورة من الملف
            history.getclearHistory();              // مسح التاريخ السابق
            updateHistoryCounter();              // تحديث عداد التراجع
            view.setZoomLevel(100);              // إعادة ضبط التكبير للواجهة
            view.getdisplayOriginalImage(processor.getOriginalImage()); // عرض الصورة الأصلية
            view.getdisplayEditedImage(processor.getCurrentImage());   // عرض الصورة المعدلة (حالياً نفس الأصلية)
        } catch (Exception e) {
            view.getshowMessage("Failed to load image"); // رسالة خطأ
        }
    }

    // ====================== SAVE IMAGE ======================
    public void gethandleSaveDialog() {
        if (processor.getCurrentImage() == null) {
            view.getshowMessage("No Image Loaded");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        String[] formats = {"png", "jpg", "jpeg", "bmp"};
        String format = (String) JOptionPane.showInputDialog(
                view, "Select format:", "Save Format",
                JOptionPane.QUESTION_MESSAGE, null, formats, "png"
        );
        if (format == null) return;

        String name = JOptionPane.showInputDialog(view, "Enter file name:");
        if (name == null || name.isEmpty()) return;

        if (chooser.showSaveDialog(view) == JFileChooser.APPROVE_OPTION) {
            String folder = chooser.getSelectedFile().getParent();
            try {
                processor.getsaveImage(folder + "/" + name + "." + format, format);
                view.getshowMessage("Image saved successfully");
            } catch (Exception e) {
                view.getshowMessage("Save failed");
            }
        }
    }

    // ====================== APPLY FILTER TEMPLATE ======================
    private void getapplyAndSave(Runnable action) {
        // تحقق من وجود صورة
        if (processor.getCurrentImage() == null) return;
        // حفظ الحالة قبل التطبيق للتمكين من Undo
        history.getsaveState(processor.getCurrentImage());
        // تنفيذ الفلتر أو التعديل
        action.run();
        // عرض الصورة بعد التعديل
        view.getdisplayEditedImage(processor.getCurrentImage());
        updateHistoryCounter();
    }

    // ====================== FILTERS ======================
    public void gethandleGrayscale() { getapplyAndSave(() -> processor.getapplyGrayscale()); }
    public void gethandleBlur() { getapplyAndSave(() -> processor.getapplyBlur()); }
    public void gethandleSepia() { getapplyAndSave(() -> processor.getapplySepia()); }
    public void gethandleSharpen() { getapplyAndSave(() -> processor.getapplySharpen()); }
    public void gethandleEdge() { getapplyAndSave(() -> processor.getapplyEdgeDetection()); }

    // ====================== ADJUSTMENTS ======================
    public void gethandleBrightness(int value) { getapplyAndSave(() -> processor.getadjustBrightness(value)); }
    public void gethandleContrast(int value) { getapplyAndSave(() -> processor.getadjustContrast(value)); }
    public void gethandleSaturation(int value) { getapplyAndSave(() -> processor.getadjustSaturation(value)); }

    // ====================== ROTATE ======================
    public void gethandleRotate(int angle) { getapplyAndSave(() -> processor.getrotateImage(angle)); }

    // ====================== CUSTOM ROTATE ======================
    public void gethandleCustomRotate() {
        if (processor.getCurrentImage() == null) {
            view.getshowMessage("Open an image first");
            return;
        }
        String input = JOptionPane.showInputDialog(view, "Enter rotation angle:");
        if (input == null || input.trim().isEmpty()) return;

        try {
            int angle = Integer.parseInt(input);
            history.getsaveState(processor.getCurrentImage());
            processor.getrotateImage(angle);
            view.getdisplayEditedImage(processor.getCurrentImage());
            updateHistoryCounter();
        } catch (NumberFormatException e) {
            view.getshowMessage("Please enter a valid number");
        }
    }

    // ====================== CROP ======================
    public void gethandleCrop() {
        if (processor.getCurrentImage() == null) return;

        Rectangle r = view.getCropRectangle(); // المنطقة المحددة للقص
        if (r == null) return;

        BufferedImage img = processor.getCurrentImage();

        int labelW = view.getEditedLabel().getWidth();  // عرض JLabel
        int labelH = view.getEditedLabel().getHeight(); // ارتفاع JLabel

        int imgW = img.getWidth(); // عرض الصورة الحقيقي
        int imgH = img.getHeight(); // ارتفاع الصورة الحقيقي

        // تحويل الإحداثيات من واجهة المستخدم للصورة الحقيقية
        double scaleX = (double) imgW / labelW;
        double scaleY = (double) imgH / labelH;

        int x = (int) (r.x * scaleX);
        int y = (int) (r.y * scaleY);
        int w = (int) (r.width * scaleX);
        int h = (int) (r.height * scaleY);

        history.getsaveState(processor.getCurrentImage());
        processor.getcropImage(x, y, w, h);
        view.getdisplayEditedImage(processor.getCurrentImage());
        view.getclearCrop();
        updateHistoryCounter();
    }

    // ====================== RESIZE ======================
    public void gethandleResize() {
        String w = JOptionPane.showInputDialog(view, "Width:");
        String h = JOptionPane.showInputDialog(view, "Height:");

        try {
            int width = Integer.parseInt(w);
            int height = Integer.parseInt(h);
            getapplyAndSave(() -> processor.getresizeImage(width, height));
        } catch (Exception e) {
            view.getshowMessage("Invalid size");
        }
    }

    // ====================== ZOOM ======================
    public void gethandleZoom(int level) {
        if (processor.getCurrentImage() == null) return;

        processor.setZoomLevel(Math.max(level, 10)); // الحد الأدنى للتكبير 10%
        view.setZoomLevel(processor.getZoomLevel());
        view.getdisplayEditedImage(processor.getCurrentImage());
    }

    // ====================== HISTORY ======================
    public void gethandleUndo() {
        BufferedImage img = history.getundo(processor.getCurrentImage());
        if (img != null) {
            processor.setCurrentImage(img);
            view.getdisplayEditedImage(img);
            updateHistoryCounter();
        }
    }

    public void gethandleRedo() {
        BufferedImage img = history.getredo(processor.getCurrentImage());
        if (img != null) {
            processor.setCurrentImage(img);
            view.getdisplayEditedImage(img);
            updateHistoryCounter();
        }
    }

    // ====================== RESET ======================
    public void gethandleReset() {
        processor.getresetImage();          // إعادة الصورة الأصلية
        history.getclearHistory();          // مسح التاريخ
        updateHistoryCounter();          // تحديث العدادات
        view.getdisplayOriginalImage(processor.getOriginalImage());
        view.getdisplayEditedImage(processor.getCurrentImage());
        view.getresetSliders();             // إعادة السلايدرز للصفر
    }

    // ====================== METADATA ======================
    public void gethandleMetadata() {
        view.getshowMessage(processor.getMetadata()); // عرض معلومات الصورة
    }

    // ====================== HISTOGRAM ======================
    public void gethandleHistogram() {
        view.getdisplayHistogram(processor.getHistogram()); // عرض الهيستوجرام
    }
}