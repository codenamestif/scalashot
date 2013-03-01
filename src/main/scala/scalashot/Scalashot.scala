package scalashot

import java.awt._
import image.BufferedImage
import swing.{Dialog, FileChooser, MainFrame, FlowPanel}
import swing.event._

import javax.swing.{UIManager, JFrame}
import javax.imageio.ImageIO
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.ByteArrayOutputStream
import org.imgscalr.Scalr
import com.cloudinary.Cloudinary
import java.util.Collections
import akka.actor.{Actor, Props, ActorSystem}

object Scalashot extends App {

  private val system = ActorSystem("System")
  private val eventHandler = system.actorOf(Props(EventHandler))

  private val screen = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit.getScreenSize))

  private val canvas = new FlowPanel() {

    focusable = true

    listenTo(mouse.clicks)
    listenTo(mouse.moves)
    listenTo(keys)

    reactions += {
      case e: MouseEntered => requestFocus()
      case e: MousePressed => eventHandler ! PressPoint(e.point.x, e.point.y)
      case e: MouseDragged => eventHandler ! DragPoint(e.point.x, e.point.y)
      case e: MouseReleased => eventHandler ! ReleasePoint(e.point.x, e.point.y)
      case e: KeyReleased if e.modifiers == Key.Modifier.Control => e.key match {
        case Key.W => eventHandler ! UploadAction
        case Key.S => eventHandler ! SaveAction
        case _ =>
      }
      case e: KeyReleased if e.key == Key.Escape => sys.exit()
    }

    override protected def paintComponent(g: Graphics2D) {
      g.drawImage(screen, 0, 0, screen.getWidth, screen.getHeight, null)
    }

  }

  private object EventHandler extends Actor {

    import context._

    def receive = {
      case PressPoint(pressX, pressY) => become({
        case DragPoint(dragX, dragY) => {
          canvas.self.getGraphics.drawImage(screen, 0, 0, screen.getWidth, screen.getHeight, null)
          canvas.self.getGraphics.drawRect(
            pressX min dragX,
            pressY min dragY,
            math.abs(dragX - pressX),
            math.abs(dragY - pressY)
          )
        }
        case ReleasePoint(releaseX, releaseY) => become({
          case UploadAction => uploadImage(Scalr.crop(screen, pressX min releaseX,
            pressY min releaseY,
            math.abs(releaseX - pressX),
            math.abs(releaseY - pressY), null))
          case SaveAction => saveImage(Scalr.crop(screen, pressX min releaseX,
            pressY min releaseY,
            math.abs(releaseX - pressX),
            math.abs(releaseY - pressY), null))
          case event => receive(event)
        })
        case event => receive(event)
      })
    }

  }

  private val frame = new MainFrame {

    resizable = false
    size = Toolkit.getDefaultToolkit.getScreenSize
    self.asInstanceOf[JFrame].setUndecorated(true)
    self.asInstanceOf[JFrame].setExtendedState(Frame.MAXIMIZED_BOTH)
    contents = canvas

  }

  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
  GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice.setFullScreenWindow(frame.peer)

  private val chooser = new FileChooser() {
    fileFilter = new FileNameExtensionFilter("*.png", "png")
  }

  private val cloudinary = new Cloudinary(args(0))

  private def saveImage(image: BufferedImage) {
    if (chooser.showSaveDialog(null) == FileChooser.Result.Approve) {
      ImageIO.write(image, "png", chooser.selectedFile)
      sys.exit()
    }
  }

  private def uploadImage(image: BufferedImage) {
    try {
      frame.dispose()
      val out = new ByteArrayOutputStream()
      ImageIO.write(image, "png", out)
      val result = cloudinary.uploader().upload(out.toByteArray, Collections.EMPTY_MAP)
      Option(result.get("secure_url").asInstanceOf[String]).map(url => {
        sendToClipBoard(url)
        infoMessage("Uploaded")
      }).getOrElse(
        Option(result.get("error").asInstanceOf[String]).foreach(errorMessage)
      )
    } catch {
      case e: Exception => errorMessage(e.getMessage)
    } finally {
      sys.exit()
    }
  }

  private def infoMessage(message: String) {
    Dialog.showMessage(message = message, messageType = Dialog.Message.Info)
  }

  private def errorMessage(message: String) {
    Dialog.showMessage(message = message, messageType = Dialog.Message.Error)
  }

  private def sendToClipBoard(text: String) {
    Runtime.getRuntime.exec("clipit " + text)
  }


}