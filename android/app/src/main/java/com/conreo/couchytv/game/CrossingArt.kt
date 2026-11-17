package com.conreo.couchytv.game

import android.graphics.*

/** Shared vector artwork for the game and collection thumbnails. No downloaded assets. */
object CrossingArt {
    fun portrait(style:Int):Bitmap = Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888).also {
        val c=Canvas(it);c.scale(1.4f,1.4f);draw(c,46f,51f,style,Paint(Paint.ANTI_ALIAS_FLAG),RectF())
    }
    fun draw(c:Canvas,x:Float,y:Float,style:Int,p:Paint,r:RectF) {
        fun box(dx:Float,dy:Float,w:Float,h:Float,color:Int,radius:Float=0f) {
            p.color=color;p.style=Paint.Style.FILL;r.set(x+dx,y+dy,x+dx+w,y+dy+h)
            c.drawRoundRect(r,radius,radius,p)
        }
        val body=CrossingCollection.skin(style).color.toInt();val ink=0xFF162D3C.toInt()
        box(-27f,17f,57f,18f,0x6609151C,10f)
        when(style) {
            10,18 -> { box(-39f,-19f,18f,32f,if(style==18)0xFFF4C770.toInt() else 0xFF5CA989.toInt(),5f);box(21f,-19f,18f,32f,if(style==18)0xFFF4C770.toInt() else 0xFF5CA989.toInt(),5f) }
            13 -> { box(-38f,-27f,20f,25f,0xFFBBDDE5.toInt(),9f);box(18f,-27f,20f,25f,0xFFBBDDE5.toInt(),9f) }
            14 -> { box(-37f,-12f,17f,22f,body,5f);box(20f,-23f,17f,25f,body,5f) }
            17 -> for(i in 0..2) { box(-37f,-25f+i*12,17f,7f,0xFFD780A3.toInt(),3f);box(20f,-25f+i*12,17f,7f,0xFFD780A3.toInt(),3f) }
        }
        box(-24f,-28f,48f,49f,body,10f)
        box(-24f,-2f,48f,23f,0x18000000,8f)
        when(style) {
            0,19 -> box(-11f,-36f,20f,12f,if(style==19)0xFFDE9C42.toInt() else 0xFFEF7D75.toInt(),4f)
            1 -> {box(-21f,-34f,17f,17f,body,5f);box(4f,-34f,17f,17f,body,5f)}
            2,15 -> {box(-2f,-40f,4f,14f,body);box(-6f,-44f,12f,8f,0xFF64D8DF.toInt(),3f)}
            3,10 -> {box(-25f,-40f,16f,20f,body,3f);box(9f,-40f,16f,20f,body,3f)}
            5 -> box(-16f,-9f,32f,29f,0xFFF1EFDD.toInt(),8f)
            6 -> {box(-25f,-35f,16f,15f,ink,6f);box(9f,-35f,16f,15f,ink,6f);box(-18f,-17f,14f,18f,ink,5f);box(4f,-17f,14f,18f,ink,5f)}
            7 -> {box(-25f,-23f,50f,12f,0xFFD97575.toInt(),3f);box(22f,-20f,17f,7f,0xFFD97575.toInt(),2f);box(-24f,0f,48f,15f,ink,4f)}
            8 -> {box(-33f,-31f,66f,10f,0xFF7461A3.toInt(),4f);box(-18f,-50f,36f,25f,0xFF8D75BD.toInt(),8f);box(-8f,-63f,16f,17f,0xFF8D75BD.toInt(),5f)}
            9 -> {box(-27f,-32f,54f,34f,0xFF8097AF.toInt(),9f);box(-3f,-47f,10f,18f,0xFFE19B85.toInt(),3f)}
            11 -> {box(-32f,-37f,64f,15f,ink,6f);box(-19f,-48f,38f,17f,ink,5f);box(-4f,-43f,8f,8f,0xFFF1E3C6.toInt(),2f)}
            12 -> {box(-27f,-33f,54f,17f,0xFF8A9BAC.toInt(),5f);box(-36f,-42f,12f,22f,0xFFE5D5AF.toInt(),4f);box(24f,-42f,12f,22f,0xFFE5D5AF.toInt(),4f)}
            13 -> {box(-24f,-1f,48f,8f,ink,2f);box(-24f,13f,48f,7f,ink,2f);box(-14f,-38f,4f,13f,ink);box(10f,-38f,4f,13f,ink)}
            14 -> {box(-13f,-20f,3f,32f,0xFF629D71.toInt());box(9f,-20f,3f,32f,0xFF629D71.toInt());box(6f,-38f,17f,13f,0xFFEEB0BD.toInt(),5f)}
            16 -> {box(-29f,-30f,58f,8f,ink,3f);box(-17f,-48f,34f,22f,ink,3f);box(-24f,9f,48f,9f,0xFFE79489.toInt(),2f);box(12f,14f,8f,21f,0xFFE79489.toInt(),2f)}
            18 -> {box(-14f,-40f,10f,17f,0xFFF4C770.toInt(),3f);box(0f,-46f,10f,22f,0xFFEF795D.toInt(),3f);box(13f,-37f,8f,13f,0xFFF4C770.toInt(),3f)}
        }
        if(style==4) {box(-20f,-21f,40f,27f,0xFF5BA9C6.toInt(),8f);box(-16f,-18f,15f,5f,0xFFB2E5EB.toInt(),2f)}
        else if(style==9 || style==15) {box(-19f,-14f,38f,10f,ink,3f);box(-13f,-11f,26f,4f,if(style==15)0xFFEFA9DC.toInt() else 0xFFB5DAE0.toInt(),2f)}
        else {box(-15f,-14f,7f,8f,if(style==6)0xFFE7E6D9.toInt() else ink,2f);box(8f,-14f,7f,8f,if(style==6)0xFFE7E6D9.toInt() else ink,2f)}
        if(style==11)box(3f,-17f,16f,14f,ink,3f)
        if(style in intArrayOf(0,5,16,18,19))box(-8f,1f,16f,8f,0xFFFFBD65.toInt(),3f)
        if(style==3)box(-13f,0f,26f,15f,0xFFF3E1C2.toInt(),5f)
        box(-16f,19f,11f,7f,0xFFD49A59.toInt(),2f);box(6f,19f,11f,7f,0xFFD49A59.toInt(),2f)
    }
}
