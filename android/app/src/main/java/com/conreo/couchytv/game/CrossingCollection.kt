package com.conreo.couchytv.game

data class CrossingSkin(val id: Int, val name: String, val price: Int, val color: Long, val description: String)
object CrossingCollection {
    val skins = listOf(
        CrossingSkin(0,"Kurča",0,0xFFF3F1DF,"Malý cestovateľ s veľkou odvahou."),
        CrossingSkin(1,"Žabka",12,0xFF99D773,"Zelená legenda mokrých ciest."),
        CrossingSkin(2,"Robot",25,0xFFADBCCB,"Napájaný odvahou. A zopár skrutkami."),
        CrossingSkin(3,"Líška",45,0xFFF0A367,"Ryšavý špecialista na tesné úniky."),
        CrossingSkin(4,"Kozmonaut",75,0xFFF3F1DF,"Jeden malý skok cez veľkú cestu."),
        CrossingSkin(5,"Tučniak",18,0xFF3A5268,"Smoking vhodný do každej zápchy."),
        CrossingSkin(6,"Panda",35,0xFFE5E4DC,"Bambus môže počkať. Auto nie."),
        CrossingSkin(7,"Ninja",60,0xFF55618B,"Tichý krok, rýchle rozhodnutia."),
        CrossingSkin(8,"Čarodej",95,0xFFAC91D8,"Červenú na semafore nezačaruje."),
        CrossingSkin(9,"Rytier",125,0xFF9AAFC3,"Brnenie je štýl, nie nesmrteľnosť."),
        CrossingSkin(10,"Dráčik",160,0xFF86C9A0,"Krídla sú zatiaľ vo výcviku."),
        CrossingSkin(11,"Pirát",200,0xFFCCAF8F,"Poklad leží na druhom brehu."),
        CrossingSkin(12,"Viking",240,0xFFBC976E,"Aj najväčší bojovník dáva prednosť vlaku."),
        CrossingSkin(13,"Včela",280,0xFFF2C75E,"Z každej výpravy trochu zlata."),
        CrossingSkin(14,"Kaktus",330,0xFF82B78B,"Ostrý vzhľad, jemné pristátie."),
        CrossingSkin(15,"Neónový robot",400,0xFF6CDAD4,"Nočná zmena nikdy nevyzerala lepšie."),
        CrossingSkin(16,"Snehuliak",460,0xFFEAF4F0,"Chladná hlava v hustej premávke."),
        CrossingSkin(17,"Axolotl",540,0xFFF0ACBD,"Ružový úsmev na každom brehu."),
        CrossingSkin(18,"Fénix",650,0xFFF29866,"Každý ďalší pokus je nový začiatok."),
        CrossingSkin(19,"Zlaté kurča",800,0xFFFFD675,"Pre tých, ktorí už prešli kus sveta.")
    )
    val ordered = skins.sortedBy { it.price }
    fun skin(id: Int) = skins.getOrElse(id) { skins[0] }
}

data class CrossingChallenge(val id:String,val title:String,val metric:String,val target:Int,val reward:Int)
object CrossingChallenges {
    val all = listOf(
        CrossingChallenge("road25","Prvý výlet","best",25,5),
        CrossingChallenge("coin5","Zlatá cesta","runCoins",5,5),
        CrossingChallenge("road50","Cez mesto","best",50,10),
        CrossingChallenge("runs10","Nevzdávam sa","runs",10,10),
        CrossingChallenge("coin12","Plné vrecká","runCoins",12,12),
        CrossingChallenge("road100","Stovkár","best",100,20),
        CrossingChallenge("earned100","Zberateľ","earned",100,20),
        CrossingChallenge("coin25","Zlatá horúčka","runCoins",25,25),
        CrossingChallenge("road200","Za horizont","best",200,35),
        CrossingChallenge("runs50","Vytrvalec","runs",50,30),
        CrossingChallenge("earned400","Malý poklad","earned",400,40),
        CrossingChallenge("road400","Legenda cesty","best",400,60),
        CrossingChallenge("earned1000","Veľký poklad","earned",1000,80),
        CrossingChallenge("road800","Bez hraníc","best",800,100)
    )
    fun value(c:CrossingChallenge,best:Int,runCoins:Int,runs:Int,earned:Int)=when(c.metric){"best"->best;"runCoins"->runCoins;"runs"->runs;else->earned}
    fun hint(c:CrossingChallenge)=when(c.metric){"best"->"Prejdi ${c.target} krokov v jednom pokuse";"runCoins"->"Nazbieraj ${c.target} mincí v jednom pokuse";"runs"->"Dokonči ${c.target} pokusov";else->"Nazbieraj spolu ${c.target} mincí"}
}
