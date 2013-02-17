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
import actors.Actor

object Scalashot extends App with Actor {

  private val screen = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit.getScreenSize))

  private val canvas = new FlowPanel() {

    focusable = true

    listenTo(mouse.clicks)
    listenTo(mouse.moves)
    listenTo(keys)

    reactions += {
      case e: MouseEntered => requestFocus()
      case e: MousePressed => Scalashot ! PressPoint(e.point.x, e.point.y)
      case e: MouseDragged => Scalashot ! DragPoint(e.point.x, e.point.y)
      case e: MouseReleased => Scalashot ! ReleasePoint(e.point.x, e.point.y)
      case e: KeyReleased if e.modifiers == Key.Modifier.Control => e.key match {
        case Key.W => Scalashot ! UploadAction
        case Key.S => Scalashot ! SaveAction
        case _ =>
      }
      case e: KeyReleased if e.key == Key.Escape => sys.exit()
    }

    override protected def paintComponent(g: Graphics2D) {
      g.drawImage(screen, 0, 0, screen.getWidth, screen.getHeight, null)
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

  start()

  def act() {
    react(action)
  }


  private def action: PartialFunction[Any, Unit] = {
    case pressPoint: PressPoint => loop(react {
      case dragPoint: DragPoint => {
        canvas.self.getGraphics.drawImage(screen, 0, 0, screen.getWidth, screen.getHeight, null)
        canvas.self.getGraphics.drawRect(
          pressPoint.x min dragPoint.x,
          pressPoint.y min dragPoint.y,
          math.abs(dragPoint.x - pressPoint.x),
          math.abs(dragPoint.y - pressPoint.y)
        )
      }
      case releasePoint: ReleasePoint => loop(react {
        case UploadAction => uploadImage(Scalr.crop(screen, pressPoint.x min releasePoint.x,
          pressPoint.y min releasePoint.y,
          math.abs(releasePoint.x - pressPoint.x),
          math.abs(releasePoint.y - pressPoint.y), null))

        case SaveAction => saveImage(Scalr.crop(screen, pressPoint.x min releasePoint.x,
          pressPoint.y min releasePoint.y,
          math.abs(releasePoint.x - pressPoint.x),
          math.abs(releasePoint.y - pressPoint.y), null))
        case pressPoint: PressPoint => action(pressPoint)
      })
      case pressPoint: PressPoint => action(pressPoint)
    })
  }

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