/*
プログラムの実行手順：
1. ターミナル / コマンドプロンプトを開く
2. build.sbt が置かれた場所で sbt と入力し、return を押す
3. project tetris と入力し、return を押す
4. run と入力し、return を押す
5. コンパイルが成功したら、tetris.A を選択（1 と入力）し、return を押す
6. ゲーム画面を閉じたら、手動で java を終了する
7. プログラムを変更後、もう一度実行したいときは run と入力し、return を押す
*/

package tetris

import scala.util.Random

import sgeometry.Pos
import sdraw.{World, Color, Transparent, HSB}

import tetris.{ShapeLib => S}
import java.beans.Transient

// テトリスを動かすための関数
case class TetrisWorld(piece: ((Int, Int), S.Shape), pile: S.Shape)( hold:S.Shape) extends World() {

  // マウスクリックは無視
  def click(p: sgeometry.Pos): World = this

  // ブロックの描画
  def drawRect(x: Int, y: Int, w: Int, h: Int, c: Color): Boolean = {
    canvas.drawRect(Pos(A.BlockSize * x, A.BlockSize * y), A.BlockSize * w, A.BlockSize * h, c)
  }

  // shape の描画（与えられた位置）
  def drawShape(pos: (Int, Int), shape: S.Shape): Boolean = {
    val pos_colors = shape.zipWithIndex.flatMap(row_i => {
      val (row, i) = row_i
      row.zipWithIndex.map(box_j => {
        val (color, j) = box_j
        (j, i, color)
      })
    })

    val (x, y) = pos
    pos_colors.forall(pos_color => {
      val (dx, dy, color) = pos_color
      drawRect(x + dx, y + dy, 1, 1, color)
    })
  }

  // shape の描画（原点）
  def drawShape00(shape: S.Shape): Boolean = drawShape((0, 0), shape)
  // ゲーム画面の描画
  val CanvasColor = HSB(0, 0, 0.1f)

  val partationA = List.fill(A.WellHeight)(List(HSB(0,0,1)))
  val partationB = List(List.fill(A.WellWidthOfSub)(HSB(0,0,1)))

//ここをいじれば描画できる
  def draw(): Boolean = {
    val (pos, shape) = piece
    val (x,y) = pos 
    canvas.drawRect(Pos(0, 0), canvas.width, canvas.height, CanvasColor) &&
    drawShape00(pile) &&
    drawShape(pos, shape) &&
    drawShape((A.WellWidth+ A.WellWidthOfSub /2 ,2),hold)&& //holdの描画
    drawShape((A.WellWidth,0),partationA)&&
    drawShape((A.WellWidth,7),partationB)&&
    drawShape( dropPiece(TetrisWorld(piece,pile)(hold)),changeColorShape(shape,HSB(0,0,0.5f)))
  }
  //新しいミノが現れる座標
  val pos = (A.WellWidth / 2 - 1, 0)

  

  // 1, 4, 7. tick
  // 目的：
  def tick(): World = {
    val ((x,y),shape)=piece
    val (h,w) = S.size(shape)
    //pileとoverlapした時も考える
    if(collision(TetrisWorld(((x,y+1),shape), pile)(hold))==false) TetrisWorld(((x,y+1),shape), pile)(hold)
    
//ここでミノは止まるから　elseの中に操作をかく（ミノが最下点に到達した時１秒猶予を与えるってのもあり？？）    
    else {
      //pileにつなげる(pieceの左上をpileに合わせる)
      
      val shape_extend = S.shiftSE(shape,x,y)
      val pileCombine = S.combine(pile,shape_extend)
      val pileNew = eraseRows(pileCombine)
      
      if(collision(TetrisWorld((pos,S.random), pileNew)(hold))) TetrisWorld((pos,shape), pile)(hold)
      else TetrisWorld((pos,S.random), pileNew)(hold)
    }
  }

  // 2, 5. keyEvent
  // 目的：
  
  def dropPiece (world:TetrisWorld):(Int,Int)={
    val TetrisWorld(((x,y),shape),pile) = world
    var yVar = y
    while(collision(TetrisWorld(((x,yVar),shape),pile)(hold))==false){
      yVar += 1
    }
   (x,yVar-1)
    }

  def newWorld(key:String):TetrisWorld={
  val ((x,y),shape)=piece
  key match{
    case "RIGHT"=>TetrisWorld(((x+1,y),shape), pile)(hold)
    case "LEFT"=>TetrisWorld(((x-1,y),shape), pile)(hold)
    case "r" =>TetrisWorld(((x,y),S.rotate(shape)),pile)(hold)
    //拡張　ミノを下に動かせる
    case "DOWN" => TetrisWorld(((x,y+1),shape),pile)(hold)
    //拡張　ハードドロップ
    case "UP" => TetrisWorld((dropPiece(TetrisWorld(((x,y),shape), pile)(hold)),shape),pile)(hold)
    case "e" => if(hold == S.empty(1,1)) TetrisWorld((pos,S.random),pile)(shape)
                else TetrisWorld((pos,hold),pile)(shape)
      }

}
def keyEvent(key: String): World = {

    val ((x,y),shape)=piece
    val new_World = newWorld(key)
    if(collision(new_World) == true) TetrisWorld(((x,y),shape), pile)(hold)
    else new_World 
   }
  
  // 3. collision
  // 目的：
  def collision(world: TetrisWorld): Boolean = {
    val ((x,y),shape)= world.piece
    val (h,w) = S.size(shape)
    val (h_pile,w_pile) = S.size(world.pile)
    val shape_extend = S.shiftSE(shape,x,y) //左上を揃える
    (x< 0)||(x+w > w_pile)||(y+h> h_pile) ||S.overlap(shape_extend,pile)
  }
  

  // 6. eraseRows
  // 目的：
  def eraseRows(pile: S.Shape): S.Shape = {
    val (h_pile,w_pile) = S.size(pile)
    val list = pile.filter(x => S.blockCountRow(x) < w_pile)
    S.empty(h_pile - list.length,w_pile)++list
    }

  def changeColorRow(row:S.Row,color:Color):S.Row={
    row.map( x => if(x==Transparent)Transparent else color)
  }
  def changeColorShape(shape:S.Shape,color:Color):S.Shape={
    shape.map(x => changeColorRow(x,color))
  }

}
  // changeColor
  

// ゲームの実行
object A extends App {
  // ゲームウィンドウとブロックのサイズ
  val WellWidth = 10
  val WellHeight = 10+10
  val BlockSize = 30
  val WellWidthOfSub = 10

  // 新しいテトロミノの作成
  val r = new Random()//オブジェクト、インスタンス

  def newPiece(): ((Int, Int), S.Shape) = {
    val pos = (WellWidth / 2 - 1, 0)
    (pos,
     List.fill(r.nextInt(4))(0).foldLeft(S.random())((shape, _) => shape))
  }

  // 最初のテトロミノ
  val piece = newPiece()

  // ゲームの初期値
  val world = TetrisWorld(piece, List.fill(WellHeight)(List.fill(WellWidth)(Transparent)))(S.empty(1,1))

  // ゲームの開始
  world.bigBang(BlockSize * (WellWidth+WellWidthOfSub), BlockSize * (WellHeight), 1)
}
