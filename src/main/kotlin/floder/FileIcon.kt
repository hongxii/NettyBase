package floder


import java.awt.FlowLayout;
import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileSystemView;

class FileIcon {

    fun getSmallIcon(f: File?): Icon? {
        if (f != null && f.exists()) {
            val fsv = FileSystemView.getFileSystemView()
            return fsv.getSystemIcon(f)
        }
        return null
    }

    fun getBigIcon(f: File?): Icon? {
        if (f != null && f.exists()) {
            try {
                val sf = sun.awt.shell.ShellFolder.getShellFolder(f)
                return ImageIcon(sf.getIcon(true))
            } catch (e: FileNotFoundException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

        }
        return null
    }

    fun main(filePath:String) {

        val f = File(filePath)
        val frm = JFrame()
        frm.setSize(300, 200)
        frm.setLocationRelativeTo(null)
        frm.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frm.isVisible = true
        frm.layout = FlowLayout(10, 10, FlowLayout.LEADING)

        val sl = JLabel("小图标")
        frm.add(sl)
        val bl = JLabel("大图标")
        frm.add(bl)

        sl.icon = getSmallIcon(f)
        bl.icon = getBigIcon(f)
    }
}