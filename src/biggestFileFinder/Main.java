package biggestFileFinder;

import javax.swing.SwingUtilities;

public class Main {

	 public static void main(String[] args) {
	        SwingUtilities.invokeLater(new Runnable() {
	            @Override
	            public void run() {
	                FolderScannerUI ui = new FolderScannerUI();
	                ui.setVisible(true);
	            }
	        });
	    }

}
