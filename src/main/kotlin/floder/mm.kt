package floder

import java.io.File
import javax.swing.filechooser.FileSystemView
import javax.swing.ImageIcon
import javax.swing.JFrame



fun main(args: Array<String>) {


    val path="D:/vsGit"
    val path2="D:/FFF"
    val file = File(path)

    val fileIcon = FileIcon()
    //fileIcon.main(path)

    val fsv = FileSystemView.getFileSystemView()

    val a=fsv.getSystemIcon(file)
    println(a.iconWidth)


    val frm = JFrame()
    val icon = ImageIcon(path2)
    frm.iconImage = icon.image


}