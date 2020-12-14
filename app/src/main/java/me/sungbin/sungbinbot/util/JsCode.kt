package me.sungbin.sungbinbot.util


/**
 * Created by SungBin on 2020-12-14.
 */

object JsCode {

    const val KOR = """
        const Kor = {
  checkSameWord: function(str, comp) {
      comp = Kor.KDivide(comp, true);
      var res = Kor.KDivide(str, true);
      var i, j = 0;
      var sim = 0;
      var per = [];
      for (var rp = 0; rp < 2; rp++) {
          for (i = 0; i < comp.length; i++) {
              for (var k = j; k < res.length; k++) {
                  if (k - j >= 2) {
                      break;
                  }
                  if (comp[i] == res[k]) {
                      sim++;
                      j = k + 1;
                      break;
                  }
              }
          }
          per[rp] = sim / (comp.length >= res.length ? comp.length : res.length) * 100;
          var temp = comp;
          comp = res;
          res = temp;
          sim = 0;
      }
      return Math.floor(per[0] >= per[1] ? per[0] : per[1]);
  },
  KDivide: function(sTest, b) {
      var rCho = ["ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"];
      var rJung = ["ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"];
      var rJong = ["", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"];
      if (b == undefined) {
          b = false;
      }
      var res = "";
      for (var i = 0; i < sTest.length; i++) {
          if (sTest[i].match(/[가-힣]/) == null) {
              res += sTest[i];
              continue;
          }
          var nTmp = sTest.charCodeAt(i) - 44032;
          var jong = nTmp % 28;
          var jung = ((nTmp - jong) / 28) % 21;
          var cho = (((nTmp - jong) / 28) - jung) / 21;
          var jungsung = rJung[jung];
          var jongsung = rJong[jong];
          if (b) {
              jongsung = jongsung.replace(/[ㅅㅈㅊㅌㅎ]/g, "ㄷ");
              jongsung = jongsung.replace(/ㄶ/g, "ㄴ");
              jongsung = jongsung.replace(/ㄻ/g, "ㅁ");
              jongsung = jongsung.replace(/ㅍ/g, "ㅂ");
              jungsung = jungsung.replace(/ㅚㅞ/g, "ㅙ");
              jungsung = jungsung.replace(/ㅔ/g, "ㅐ");
          }
          res += rCho[cho] + jungsung + jongsung;
      }
      return res;
  }
};
    """

}