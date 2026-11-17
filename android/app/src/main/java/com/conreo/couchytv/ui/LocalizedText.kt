package com.conreo.couchytv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.TextUnit
import androidx.tv.material3.MaterialTheme
import com.conreo.couchytv.AppLanguage

@Composable
fun LocalizedText(text:String,modifier:Modifier=Modifier,color:Color=Color.Unspecified,
    fontSize:TextUnit=TextUnit.Unspecified,fontStyle:FontStyle?=null,fontWeight:FontWeight?=null,
    fontFamily:FontFamily?=null,letterSpacing:TextUnit=TextUnit.Unspecified,textDecoration:TextDecoration?=null,
    textAlign:TextAlign?=null,lineHeight:TextUnit=TextUnit.Unspecified,overflow:TextOverflow=TextOverflow.Clip,
    softWrap:Boolean=true,maxLines:Int=Int.MAX_VALUE,minLines:Int=1,onTextLayout:(TextLayoutResult)->Unit={},
    style:TextStyle=MaterialTheme.typography.bodyLarge) {
    androidx.tv.material3.Text(AppLanguage.text(text),modifier,color,fontSize,fontStyle,fontWeight,fontFamily,
        letterSpacing,textDecoration,textAlign,lineHeight,overflow,softWrap,maxLines,minLines,onTextLayout,style)
}

@Composable
fun LocalizedText(text:AnnotatedString,modifier:Modifier=Modifier,color:Color=Color.Unspecified,
    fontSize:TextUnit=TextUnit.Unspecified,fontStyle:FontStyle?=null,fontWeight:FontWeight?=null,
    fontFamily:FontFamily?=null,letterSpacing:TextUnit=TextUnit.Unspecified,textDecoration:TextDecoration?=null,
    textAlign:TextAlign?=null,lineHeight:TextUnit=TextUnit.Unspecified,overflow:TextOverflow=TextOverflow.Clip,
    softWrap:Boolean=true,maxLines:Int=Int.MAX_VALUE,minLines:Int=1,
    inlineContent:Map<String,androidx.compose.foundation.text.InlineTextContent> = mapOf(),
    onTextLayout:(TextLayoutResult)->Unit={},style:TextStyle=MaterialTheme.typography.bodyLarge) {
    androidx.tv.material3.Text(text,modifier,color,fontSize,fontStyle,fontWeight,fontFamily,
        letterSpacing,textDecoration,textAlign,lineHeight,overflow,softWrap,maxLines,minLines,inlineContent,onTextLayout,style)
}
