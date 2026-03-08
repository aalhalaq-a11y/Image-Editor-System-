package model;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Stack;

public class HistoryManager {

    // ====================== ATTRIBUTES ======================
    private final Stack<BufferedImage> undoStack = new Stack<>(); // لتخزين الحالات السابقة (Undo)
    private final Stack<BufferedImage> redoStack = new Stack<>(); // لتخزين الحالات التي تم التراجع عنها (Redo)

    // الحد الأقصى لعدد الحالات في التاريخ لتجنب استهلاك الذاكرة الزائد
    private static final int MAX_HISTORY = 50;

    // ====================== SAVE STATE ======================
    // حفظ نسخة من الصورة قبل أي تعديل
    public void getsaveState(BufferedImage image) {
        if (image == null) return;

        // إذا وصلنا الحد الأقصى، إزالة أقدم حالة
        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.remove(0);
        }

        // حفظ نسخة عميقة من الصورة لمنع التعديل على الأصل
        undoStack.push(getdeepCopy(image));

        // أي تعديل جديد يلغي كل redo المتاح
        redoStack.clear();
    }

    // ====================== UNDO ======================
    // التراجع عن آخر تعديل
    public BufferedImage getundo(BufferedImage currentImage) {

        if (undoStack.isEmpty())
            return null; // لا يوجد شيء للتراجع عنه

        // حفظ الحالة الحالية في redo قبل التراجع
        if (currentImage != null)
            redoStack.push(getdeepCopy(currentImage));

        // إعادة آخر حالة محفوظة
        return undoStack.pop();
    }

    // ====================== REDO ======================
    // إعادة آخر تعديل تم التراجع عنه
    public BufferedImage getredo(BufferedImage currentImage) {

        if (redoStack.isEmpty())
            return null; // لا يوجد شيء لإعادة تطبيقه

        // حفظ الحالة الحالية في undo قبل إعادة التعديل
        if (currentImage != null)
            undoStack.push(getdeepCopy(currentImage));

        return redoStack.pop();
    }

    // ====================== CAN UNDO ======================
    public boolean getcanUndo() {
        return !undoStack.isEmpty(); // هل يوجد أي تعديل سابق يمكن التراجع عنه؟
    }

    // ====================== CAN REDO ======================
    public boolean getcanRedo() {
        return !redoStack.isEmpty(); // هل يوجد أي تعديل تم التراجع عنه يمكن إعادة تطبيقه؟
    }

    // ====================== COUNTERS ======================
    public int getUndoCount() {
        return undoStack.size(); // عدد التعديلات المتاحة للتراجع
    }

    public int getRedoCount() {
        return redoStack.size(); // عدد التعديلات المتاحة لإعادة التطبيق
    }

    // ====================== CLEAR HISTORY ======================
    // مسح كل التاريخ (Undo و Redo)
    public void getclearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    // ====================== DEEP COPY ======================
    // عمل نسخة كاملة من BufferedImage لمنع التعديلات على النسخة الأصلية
    private BufferedImage getdeepCopy(BufferedImage img) {
        ColorModel cm = img.getColorModel();        // الحصول على نموذج الألوان
        boolean alpha = cm.isAlphaPremultiplied();  // التحقق من وجود قناة ألفا
        WritableRaster raster = img.copyData(null); // نسخ بيانات الصورة

        return new BufferedImage(cm, raster, alpha, null); // إنشاء نسخة جديدة
    }
}