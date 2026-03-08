package view;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class MainFrame extends JFrame {

    private final JLabel originalLabel;
    private final JLabel editedLabel;

    private final JSplitPane splitPane;

    // Buttons
    private final JButton openBtn, saveBtn, metaBtn;
    private final JButton grayBtn, blurBtn, sepiaBtn, sharpenBtn, edgeBtn;
    private final JButton undoBtn, redoBtn;
    private final JButton zoomInBtn, zoomOutBtn;
    private final JButton rotateLeftBtn, rotateRightBtn;
    private final JButton resetBtn,  histogramBtn;
    private final JButton  resizeBtn, cropBtn;
    private final JButton rotateCustomBtn;
    // Sliders
    private final JSlider brightnessSlider;
    private final JSlider contrastSlider;
    private final JSlider saturationSlider;

    private final JLabel historyLabel;

    private int zoomLevel = 100;

    private BufferedImage originalDisplayedImage;
    private BufferedImage editedDisplayedImage;

    // Crop
    private Point cropStart;
    private Point cropEnd;
    private Rectangle cropRectangle;

    // Pan
    private Point dragStart;

    // Drag Drop
    private DropFileListener dropListener;

    public interface DropFileListener {
        void onFileDropped(File file);
    }

    public void setDropFileListener(DropFileListener listener) {
        this.dropListener = listener;
    }

    public MainFrame() {

        setTitle("Smart Image Editor - Professional Version");
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

       JPanel toolBar1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
JPanel toolBar2 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Buttons
        openBtn = new JButton("Open");
        saveBtn = new JButton("Save");
        metaBtn = new JButton("Metadata");

        grayBtn = new JButton("Grayscale");
        blurBtn = new JButton("Blur");
        sepiaBtn = new JButton("Sepia");
        sharpenBtn = new JButton("Sharpen");
        edgeBtn = new JButton("Edge");

        undoBtn = new JButton("Undo");
        redoBtn = new JButton("Redo");


        zoomInBtn = new JButton("Zoom +");
        zoomOutBtn = new JButton("Zoom -");

        rotateLeftBtn = new JButton("Rotate ⟲");
        rotateRightBtn = new JButton("Rotate ⟳");
rotateCustomBtn = new JButton("Rotate Custom");
        resetBtn = new JButton("Reset");
 
        histogramBtn = new JButton("Histogram");

     
        resizeBtn = new JButton("Resize");
cropBtn = new JButton("Crop");

        brightnessSlider = new JSlider(-100, 100, 0);
        contrastSlider = new JSlider(-100, 100, 0);
        saturationSlider = new JSlider(-100, 100, 0);

        brightnessSlider.setPreferredSize(new Dimension(120,40));
        contrastSlider.setPreferredSize(new Dimension(120,40));
        saturationSlider.setPreferredSize(new Dimension(120,40));

        historyLabel = new JLabel("Undo: 0 | Redo: 0");

        // Add buttons
        toolBar1.add(openBtn);
toolBar1.add(saveBtn);

toolBar1.add(metaBtn);

toolBar1.add(grayBtn);
toolBar1.add(blurBtn);
toolBar1.add(sepiaBtn);
toolBar1.add(sharpenBtn);
toolBar1.add(edgeBtn);

toolBar1.add(undoBtn);
toolBar1.add(redoBtn);


toolBar1.add(zoomInBtn);
toolBar1.add(zoomOutBtn);

toolBar1.add(rotateLeftBtn);
toolBar1.add(rotateRightBtn);
toolBar1.add(rotateCustomBtn);

toolBar2.add(resetBtn);

toolBar2.add(histogramBtn);


toolBar2.add(resizeBtn);
toolBar2.add(cropBtn);

toolBar2.add(new JLabel("Brightness"));
toolBar2.add(brightnessSlider);

toolBar2.add(new JLabel("Contrast"));
toolBar2.add(contrastSlider);

toolBar2.add(new JLabel("Saturation"));
toolBar2.add(saturationSlider);

toolBar2.add(historyLabel);
JPanel topPanel = new JPanel(new BorderLayout());

topPanel.add(toolBar1, BorderLayout.NORTH);
topPanel.add(toolBar2, BorderLayout.SOUTH);

add(topPanel, BorderLayout.NORTH);

        // Image labels
        originalLabel = new JLabel("Original Image", SwingConstants.CENTER);
originalLabel.setPreferredSize(new Dimension(100,100));

        editedLabel = new JLabel("Edited Image", SwingConstants.CENTER)
                
                
        {

            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                if (cropStart != null && cropEnd != null) {

                    g.setColor(Color.RED);

                    int x = Math.min(cropStart.x, cropEnd.x);
                    int y = Math.min(cropStart.y, cropEnd.y);
                    int w = Math.abs(cropStart.x - cropEnd.x);
                    int h = Math.abs(cropStart.y - cropEnd.y);

                    g.drawRect(x,y,w,h);
                }
            }
        };
JScrollPane leftScroll = new JScrollPane(originalLabel);
JScrollPane rightScroll = new JScrollPane(editedLabel);

splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,leftScroll,rightScroll);
splitPane.setResizeWeight(0.5);

// تحديد ارتفاع الصور
splitPane.setPreferredSize(new Dimension(1000,350));

add(splitPane,BorderLayout.CENTER);

        // Zoom using mouse wheel
        editedLabel.addMouseWheelListener(e -> {

            if(e.getWheelRotation() < 0)
                zoomLevel += 10;
            else
                zoomLevel -= 10;

            zoomLevel = Math.max(10,Math.min(500,zoomLevel));

            refreshImages();
        });

        // Mouse actions
        editedLabel.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {

                if(SwingUtilities.isLeftMouseButton(e))
                    cropStart = e.getPoint();

                dragStart = e.getPoint();
            }

            public void mouseReleased(MouseEvent e) {

                cropEnd = e.getPoint();

                if(cropStart != null && cropEnd != null){

                    cropRectangle = new Rectangle(
                            Math.min(cropStart.x,cropEnd.x),
                            Math.min(cropStart.y,cropEnd.y),
                            Math.abs(cropStart.x-cropEnd.x),
                            Math.abs(cropStart.y-cropEnd.y)
                    );
                }

                repaint();
            }
        });

        editedLabel.addMouseMotionListener(new MouseMotionAdapter() {

            public void mouseDragged(MouseEvent e) {

                if(cropStart != null){

                    cropEnd = e.getPoint();
                    repaint();
                }

            JViewport viewport =
        (JViewport) SwingUtilities.getAncestorOfClass(
                JViewport.class,
                editedLabel
        );

                Point p = viewport.getViewPosition();

                int dx = dragStart.x - e.getX();
                int dy = dragStart.y - e.getY();

                p.translate(dx,dy);

                editedLabel.scrollRectToVisible(new Rectangle(p,viewport.getSize()));
            }
        });

        // Drag & Drop
        new DropTarget(this,new DropTargetAdapter(){

            public void drop(DropTargetDropEvent evt){

                try{

                    evt.acceptDrop(DnDConstants.ACTION_COPY);

                    List<File> files =
                            (List<File>)evt.getTransferable()
                                    .getTransferData(DataFlavor.javaFileListFlavor);

                    if(!files.isEmpty() && dropListener != null)
                        dropListener.onFileDropped(files.get(0));

                }catch(Exception ex){

                    getshowMessage("Drag & Drop Failed");
                }
            }
        });

        setVisible(true);
    }

    // Display images
    public void getdisplayOriginalImage(BufferedImage image){
        originalDisplayedImage = image;
        refreshImages();
    }

    public void getdisplayEditedImage(BufferedImage image){
        editedDisplayedImage = image;
        refreshImages();
    }

private void refreshImages(){

    if(originalDisplayedImage != null){
        originalLabel.setIcon(new ImageIcon(originalDisplayedImage));
        originalLabel.setText("");
    }

    if(editedDisplayedImage != null){
        editedLabel.setIcon(new ImageIcon(getscaleImage(editedDisplayedImage)));
        editedLabel.setText("");
    }
}

    private Image getscaleImage(BufferedImage image){

        int w = image.getWidth()*zoomLevel/100;
        int h = image.getHeight()*zoomLevel/100;

        return image.getScaledInstance(w,h,Image.SCALE_SMOOTH);
    }

public Rectangle getCropRectangle() {

    if (cropStart == null || cropEnd == null)
        return null;

    return new Rectangle(
            Math.min(cropStart.x, cropEnd.x),
            Math.min(cropStart.y, cropEnd.y),
            Math.abs(cropStart.x - cropEnd.x),
            Math.abs(cropStart.y - cropEnd.y)
    );
}
public JLabel getEditedLabel() {
    return editedLabel;
}
   public void getclearCrop(){

    cropStart = null;
    cropEnd = null;
    cropRectangle = null;

    repaint();
}

    public void getshowMessage(String msg){
        JOptionPane.showMessageDialog(this,msg);
    }

    public void setZoomLevel(int zoom){
        zoomLevel = zoom;
        refreshImages();
    }

    public void getupdateHistoryCount(int undo,int redo){
        historyLabel.setText("Undo: "+undo+" | Redo: "+redo);
    }

    public void getdisplayHistogram(int[] histogram){

        JFrame frame = new JFrame("Histogram");
        frame.setSize(600,400);

        JPanel panel = new JPanel(){

            protected void paintComponent(Graphics g){

                super.paintComponent(g);

                int w = getWidth();
                int h = getHeight();

                int max = 0;

                for(int v:histogram)
                    if(v>max) max=v;

                int barWidth = (int)Math.ceil((double)w/histogram.length);

                for(int i=0;i<histogram.length;i++){

                    int barHeight =
                            (int)((double)histogram[i]/max*h);

                    g.drawLine(
                            i*barWidth,
                            h,
                            i*barWidth,
                            h-barHeight
                    );
                }
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }
 
    public void getresetSliders() {

    brightnessSlider.setValue(0);
    contrastSlider.setValue(0);
    saturationSlider.setValue(0);
}
    public JButton getCropBtn() {
    return cropBtn;
}
    

    // Getters
    public JButton getOpenBtn(){return openBtn;}
    public JButton getSaveBtn(){return saveBtn;}
    public JButton getMetaBtn(){return metaBtn;}

    public JButton getGrayBtn(){return grayBtn;}
    public JButton getBlurBtn(){return blurBtn;}
    public JButton getSepiaBtn(){return sepiaBtn;}
    public JButton getSharpenBtn(){return sharpenBtn;}
    public JButton getEdgeBtn(){return edgeBtn;}

    public JButton getUndoBtn(){return undoBtn;}
    public JButton getRedoBtn(){return redoBtn;}


    public JButton getZoomInBtn(){return zoomInBtn;}
    public JButton getZoomOutBtn(){return zoomOutBtn;}

    public JButton getRotateLeftBtn(){return rotateLeftBtn;}
    public JButton getRotateRightBtn(){return rotateRightBtn;}

    public JButton getResetBtn(){return resetBtn;}
   
    public JButton getHistogramBtn(){return histogramBtn;}
   
    public JButton getResizeBtn(){return resizeBtn;}
    public JButton getRotateCustomBtn(){return rotateCustomBtn;}
    public JSlider getBrightnessSlider(){return brightnessSlider;}
    public JSlider getContrastSlider(){return contrastSlider;}
    public JSlider getSaturationSlider(){return saturationSlider;}
    
}
