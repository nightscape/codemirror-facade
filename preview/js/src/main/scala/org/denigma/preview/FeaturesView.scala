package org.denigma.preview

import editors.CodeMirrorEditor
import org.denigma.binding.binders.ReactiveBinder
import org.denigma.binding.views.BindableView
import org.denigma.codemirror.{LineInfo, Editor, CodeMirror, EditorConfiguration}
import org.denigma.codemirror.extensions.EditorConfig
import org.scalajs.dom
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import rx.Var
import scala.scalajs.js.JSConverters._
import scala.scalajs.js
import org.denigma.codemirror._
import org.denigma.codemirror.extensions._
import scalatags.JsDom.all._
import org.denigma.binding.extensions._
import scalatags.JsDom.all._
import fastparse.all._


/**
  * Created by antonkulaga on 2/24/16.
  */
class FeaturesView(val elem: Element) extends BindableView with EditorView with ExampleKappaData{

  //val linkParser = P( "a" )

  "a" ~ "b"


  val defaultText: String = kappaCode

  override def mode = "Kappa"

  override def addEditor(name: String, element: ViewElement, codeMode: String): Unit = element match {
    case area: HTMLTextAreaElement =>
      val text = if (area.value == "") defaultText else area.value
      editor = this.makeEditor(area, area.value, codeMode)

    case _ =>
      val message = "cannot find text area for the code!"
      throw new Exception(message)
  }

  protected def makeMarker(link: String): Anchor = {
    val tag = a(href := link,
      i(`class` := "file pdf outline icon")
    )
    tag.render

    // <i class="file pdf outline icon">
  }

  changes.onChange{
    case ch =>
      val lines = ch.newLines
      for( (num, line) <- lines){
        println(s"$num : $line")
        val marker = this.makeMarker("https://codemirror.net/demo/marker.html")
        editor.setGutterMarker(num, "breakpoints", marker)
      }
      println(s"change is $ch")
  }

  gutterClicks.onChange{
    case line =>
      val marker = this.makeMarker("https://codemirror.net/demo/marker.html")
      editor.setGutterMarker(line, "breakpoints", marker)
    //println(s"gutter click on $line")

  }
}

trait EditorView extends BindableView with EditorMaker with WithMirrors{

  def mode: String = "htmlmixed"

  private var _editor: Editor = null
  def editor: Editor = {
    if (_editor == null) dom.console.error("editor is NULL!")
    _editor
  }

  def editor_=(value: Editor): Unit = {
    _editor = value
    subscribeEditor(_editor)
  }

  protected def subscribeEditor(editor: Editor) = {
    def onChange(ed: Editor, change: EditorChangeLike): Unit = {
      changes() = change
    }
    def onGutterClick(ed: Editor, line: Int): Unit = {
      gutterClicks() = line
    }
    editor.addOnChange(onChange)
    editor.addOnGutterClick(onGutterClick)
  }

  lazy val changes: Var[EditorChangeLike] = Var(EditorChangeLike.empty)
  lazy val gutterClicks: Var[Int] = Var(0)

  def contains(name: String): Boolean = if (_editor==null) false else {
    println("warning: editor view already contains an editor")
    true
  }

  withBinder(new EditorsBinder(_, mode))

}


trait WithMirrors extends BindableView {

  def contains(name: String): Boolean

  def addEditor(name: String, element: Element, codeMode: String): Unit

}


trait EditorMaker {

  def makeEditor(area: HTMLTextAreaElement, textValue: String, codeMode: String, readOnly: Boolean = false): Editor = {
    val params = EditorConfig
      .mode(codeMode)
      .lineNumbers(true)
      .value(textValue)
      .readOnly(readOnly)
      .viewportMargin(Integer.MAX_VALUE)
      .gutters(js.Array("CodeMirror-linenumbers", "breakpoints"))
    //  gutters: ["CodeMirror-linenumbers", "breakpoints"]

    CodeMirror.fromTextArea(area, params)
  }

}

class EditorsBinder(view: WithMirrors, defaultMode: String = "htmlmixed") extends ReactiveBinder
{

  override def bindAttributes(el: Element, attributes: Map[String, String]): Boolean= {
    val ats = this.dataAttributesOnly(attributes)
    val fun: PartialFunction[(String, String), Unit] = elementPartial(el, ats).orElse{case other =>}
    ats.foreach(fun)
    true
  }

  override def elementPartial(el: Element, ats: Map[String, String]): PartialFunction[(String, String), Unit] = {
    case ("editor", v) if !view.contains(v) =>
      //println(s"adding editor for name $value")
      view.addEditor(v, el, ats.getOrElse("mode", defaultMode))

  }
}