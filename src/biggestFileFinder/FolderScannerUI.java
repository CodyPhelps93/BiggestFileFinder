package biggestFileFinder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.text.DecimalFormat;

public class FolderScannerUI extends JFrame {
    private JTextArea resultArea;
    private JTextArea deniedListArea;
    private JButton selectDirectoryButton;
    private JButton stopScanButton;
    private JFileChooser fileChooser;
    private JProgressBar progressBar;
    private volatile boolean scanning;
    private Thread scanThread;
    List<String> deniedPaths = new ArrayList<>();

    public FolderScannerUI() {
        setTitle("Drive Folder Scanner");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        deniedListArea = new JTextArea();
        deniedListArea.setEditable(false);
        JScrollPane deniedScrollPane = new JScrollPane(deniedListArea);

        selectDirectoryButton = new JButton("Select Directory");
        selectDirectoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectDrive();
            }
        });

        stopScanButton = new JButton("Stop Scan");
        stopScanButton.setEnabled(false);
        stopScanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopScan();
            }
        });

        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(selectDirectoryButton);
        buttonPanel.add(stopScanButton);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, deniedScrollPane);
        splitPane.setResizeWeight(0.7);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);

        add(panel);
    }

    private void selectDrive() {
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            Path selectedPath = fileChooser.getSelectedFile().toPath();
            startScan(selectedPath);
        }
    }

    private void startScan(Path startPath) {
        scanning = true;
        stopScanButton.setEnabled(true);
        resultArea.setText("Scanning drive...\n");
        deniedListArea.setText(""); // Clear denied list area
        deniedPaths.clear();
        progressBar.setValue(0); // Reset progress bar

        scanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<FolderInfo> folderInfoList = new ArrayList<>();
                

                try {
                    DirectoryStream<Path> stream = Files.newDirectoryStream(startPath);
                    long totalDirs = countDirectories(startPath);
                    System.out.println(totalDirs);
                    progressBar.setMaximum((int) totalDirs);

                    int dirsScanned = 0;
                    

                    for (Path subDir : stream) {
                    	final int progress = dirsScanned / (int) totalDirs;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                progressBar.setValue(progress);
                            }
                        });
                        if (!scanning) {
                            break;
                        }
                        if (Files.isDirectory(subDir)) {
                            try {
                                long size = calculateFolderSize(subDir);
                                FileTime lastModifiedTime = Files.getLastModifiedTime(subDir);
                                folderInfoList.add(new FolderInfo(subDir, size, lastModifiedTime));
                            } catch (AccessDeniedException e) {
                               
                                System.err.println("Access denied to: " + subDir.toString());
                            }
                        }
                        dirsScanned++;
                        
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                folderInfoList.sort(Comparator.comparingLong(folder -> -folder.getSize()));

                int topN = 10;
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < Math.min(topN, folderInfoList.size()); i++) {
                    FolderInfo folder = folderInfoList.get(i);
                    result.append("Folder: ").append(folder.getPath()).append("\n");
                    result.append("Size: ").append(formatSize(folder.getSize())).append("\n");
                    result.append("Last Modified: ").append(folder.getLastModifiedTime()).append("\n\n");
                }

               

                StringBuilder deniedPathsString = new StringBuilder();
                for (String path : deniedPaths) {
                    deniedPathsString.append(path).append("\n");
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        resultArea.setText(result.toString());
                        deniedListArea.setText(deniedPathsString.toString());
                        progressBar.setValue(progressBar.getMaximum());
                        stopScanButton.setEnabled(false);

                        // Additional debug output
                        System.out.println("Denied paths set in JTextArea:");
                        System.out.println(deniedPathsString.toString());
                    }
                });
            }
        });

        scanThread.start();
    }


    private void stopScan() {
        scanning = false;
        stopScanButton.setEnabled(false);
        resultArea.append("\nScan stopped.");
        if (scanThread != null && scanThread.isAlive()) {
            scanThread.interrupt();
        }
    }

    private long countDirectories(Path startPath) {
        final long[] count = {0};
        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    count[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    if (exc instanceof AccessDeniedException) {
                        System.err.println("Access denied to file: " + file.toString());
                        deniedPaths.add("Access denied to: " + file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count[0];
    }

    private long calculateFolderSize(Path dir) throws IOException {
        final long[] size = {0};
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!scanning) {
                    return FileVisitResult.TERMINATE;
                }
                size[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                if (exc instanceof AccessDeniedException) {
                    System.err.println("Access denied to file: " + file.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return size[0];
    }

    private String formatSize(long bytes) {
        final long KILOBYTE = 1024;
        final long MEGABYTE = KILOBYTE * 1024;
        final long GIGABYTE = MEGABYTE * 1024;

        DecimalFormat df = new DecimalFormat("#.##");

        if (bytes >= GIGABYTE) {
            return df.format((double) bytes / GIGABYTE) + " GB";
        } else if (bytes >= MEGABYTE) {
            return df.format((double) bytes / MEGABYTE) + " MB";
        } else if (bytes >= KILOBYTE) {
            return df.format((double) bytes / KILOBYTE) + " KB";
        } else {
            return bytes + " bytes";
        }
    }

}
