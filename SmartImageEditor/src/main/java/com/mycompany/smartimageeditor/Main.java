package com.mycompany.smartimageeditor;

import controller.Controller;
import model.ImageProcessor;
import model.HistoryManager;
import view.MainFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        // تشغيل البرنامج داخل Event Dispatch Thread (أفضل ممارسة في Swing)
        SwingUtilities.invokeLater(() -> {

            try {
                // تفعيل شكل النظام (شكل الويندوز / الماك حسب الجهاز)
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            // ===== MVC Components =====
            ImageProcessor model = new ImageProcessor();
            HistoryManager history = new HistoryManager();
            MainFrame view = new MainFrame();

            new Controller(view, model, history);

        });
    }
}