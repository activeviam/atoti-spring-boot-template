(()=>{"use strict";var e={1477:(e,t,r)=>{r(2980),r(8879),r(4346);var n=r(6535),i=r(2485);i.pluginMenuItemFilterOnEverythingButSelection.key,i.pluginMenuItemFilterOnSelection.key,i.pluginMenuItemOpenDrillthrough.key,i.pluginMenuItemInvestigate.key,i.pluginMenuItemRemoveSort.key,i.pluginMenuItemCopyQuery.key,i.pluginMenuItemShowHideTotals.key,i.pluginMenuItemRefreshQuery.key,i.pluginMenuItemExportToCsv.key,i.pluginMenuItemHideColumns.key,i.pluginMenuItemSwitchWidgetType.key,i.pluginMenuItemRemoveWidget.key,i.pluginMenuItemDuplicateWidget.key,i.pluginMenuItemSynchronizeSavedWidget.key,i.pluginMenuItemSaveWidget.key,i.pluginMenuItemSaveWidgetAs.key,i.pluginTitleBarButtonFullScreen.key,i.pluginTitleBarButtonToggleQueryMode.key,(0,n.ht)(!1),(0,i.validateEnv)()}},t={};function r(n){var i=t[n];if(void 0!==i)return i.exports;var a=t[n]={exports:{}};return e[n].call(a.exports,a,a.exports,r),a.exports}r.m=e,r.c=t,(()=>{var e=[];r.O=(t,n,i,a)=>{if(n){a=a||0;for(var o=e.length;o>0&&e[o-1][2]>a;o--)e[o]=e[o-1];e[o]=[n,i,a];return}for(var u=1/0,o=0;o<e.length;o++){for(var[n,i,a]=e[o],l=!0,s=0;s<n.length;s++)(!1&a||u>=a)&&Object.keys(r.O).every(e=>r.O[e](n[s]))?n.splice(s--,1):(l=!1,a<u&&(u=a));if(l){e.splice(o--,1);var f=i();void 0!==f&&(t=f)}}return t}})(),r.n=e=>{var t=e&&e.__esModule?()=>e.default:()=>e;return r.d(t,{a:t}),t},r.d=(e,t)=>{for(var n in t)r.o(t,n)&&!r.o(e,n)&&Object.defineProperty(e,n,{enumerable:!0,get:t[n]})},r.f={},r.e=e=>Promise.all(Object.keys(r.f).reduce((t,n)=>(r.f[n](e,t),t),[])),r.u=e=>"static/js/async/"+e+"."+({184:"1211e9dd",349:"9d182ff6"})[e]+".js",r.miniCssF=e=>{},r.g=function(){if("object"==typeof globalThis)return globalThis;try{return this||Function("return this")()}catch(e){if("object"==typeof window)return window}}(),r.o=(e,t)=>Object.prototype.hasOwnProperty.call(e,t),(()=>{var e={},t="@activeviam/limits-ui:";r.l=(n,i,a,o)=>{if(e[n]){e[n].push(i);return}if(void 0!==a)for(var u,l,s=document.getElementsByTagName("script"),f=0;f<s.length;f++){var p=s[f];if(p.getAttribute("src")==n||p.getAttribute("data-webpack")==t+a){u=p;break}}u||(l=!0,(u=document.createElement("script")).charset="utf-8",u.timeout=120,r.nc&&u.setAttribute("nonce",r.nc),u.setAttribute("data-webpack",t+a),u.src=n),e[n]=[i];var d=(t,r)=>{u.onerror=u.onload=null,clearTimeout(c);var i=e[n];if(delete e[n],u.parentNode&&u.parentNode.removeChild(u),i&&i.forEach(e=>e(r)),t)return t(r)},c=setTimeout(d.bind(null,void 0,{type:"timeout",target:u}),12e4);u.onerror=d.bind(null,u.onerror),u.onload=d.bind(null,u.onload),l&&document.head.appendChild(u)}})(),r.r=e=>{"undefined"!=typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(e,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(e,"__esModule",{value:!0})},(()=>{r.S={};var e={},t={};r.I=(n,i)=>{i||(i=[]);var a=t[n];if(a||(a=t[n]={}),!(i.indexOf(a)>=0)){if(i.push(a),e[n])return e[n];r.o(r.S,n)||(r.S[n]={}),r.S[n];var o=[];return o.length?e[n]=Promise.all(o).then(()=>e[n]=1):e[n]=1}}})(),(()=>{r.g.importScripts&&(e=r.g.location+"");var e,t=r.g.document;if(!e&&t&&(t.currentScript&&"SCRIPT"===t.currentScript.tagName.toUpperCase()&&(e=t.currentScript.src),!e)){var n=t.getElementsByTagName("script");if(n.length)for(var i=n.length-1;i>-1&&(!e||!/^http(s?):/.test(e));)e=n[i--].src}if(!e)throw Error("Automatic publicPath is not supported in this browser");e=e.replace(/#.*$/,"").replace(/\?.*$/,"").replace(/\/[^\/]+$/,"/"),r.p=e+"../../"})(),(()=>{var e=e=>{var t=e=>e.split(".").map(e=>+e==e?+e:e),r=/^([^-+]+)?(?:-([^+]+))?(?:\+(.+))?$/.exec(e),n=r[1]?t(r[1]):[];return r[2]&&(n.length++,n.push.apply(n,t(r[2]))),r[3]&&(n.push([]),n.push.apply(n,t(r[3]))),n},t=(t,r)=>{t=e(t),r=e(r);for(var n=0;;){if(n>=t.length)return n<r.length&&"u"!=(typeof r[n])[0];var i=t[n],a=(typeof i)[0];if(n>=r.length)return"u"==a;var o=r[n],u=(typeof o)[0];if(a!=u)return"o"==a&&"n"==u||"s"==u||"u"==a;if("o"!=a&&"u"!=a&&i!=o)return i<o;n++}},n=e=>{var t=e[0],r="";if(1===e.length)return"*";if(t+.5){r+=0==t?">=":-1==t?"<":1==t?"^":2==t?"~":t>0?"=":"!=";for(var i=1,a=1;a<e.length;a++)i--,r+="u"==(typeof(u=e[a]))[0]?"-":(i>0?".":"")+(i=2,u);return r}var o=[];for(a=1;a<e.length;a++){var u=e[a];o.push(0===u?"not("+l()+")":1===u?"("+l()+" || "+l()+")":2===u?o.pop()+" "+o.pop():n(u))}return l();function l(){return o.pop().replace(/^\((.+)\)$/,"$1")}},i=(t,r)=>{if(0 in t){r=e(r);var n=t[0],a=n<0;a&&(n=-n-1);for(var o=0,u=1,l=!0;;u++,o++){var s,f,p=u<t.length?(typeof t[u])[0]:"";if(o>=r.length||"o"==(f=(typeof(s=r[o]))[0]))return!l||("u"==p?u>n&&!a:""==p!=a);if("u"==f){if(!l||"u"!=p)return!1}else if(l){if(p==f){if(u<=n){if(s!=t[u])return!1}else{if(a?s>t[u]:s<t[u])return!1;s!=t[u]&&(l=!1)}}else if("s"!=p&&"n"!=p){if(a||u<=n)return!1;l=!1,u--}else{if(u<=n||f<p!=a)return!1;l=!1}}else"s"!=p&&"n"!=p&&(l=!1,u--)}}var d=[],c=d.pop.bind(d);for(o=1;o<t.length;o++){var v=t[o];d.push(1==v?c()|c():2==v?c()&c():v?i(v,r):!c())}return!!c()},a=(e,t)=>e&&r.o(e,t),o=e=>(e.loaded=1,e.get()),u=e=>Object.keys(e).reduce((t,r)=>(e[r].eager&&(t[r]=e[r]),t),{}),l=(e,r,n)=>{var i=n?u(e[r]):e[r];return Object.keys(i).reduce((e,r)=>!e||!i[e].loaded&&t(e,r)?r:e,0)},s=(e,t,r,i)=>"Unsatisfied version "+r+" from "+(r&&e[t][r].from)+" of shared singleton module "+t+" (required "+n(i)+")",f=e=>{throw Error(e)},p=(e,t)=>f("Shared module "+t+" doesn't exist in shared scope "+e),d=(e,t,r)=>r?r():p(e,t),c=/*#__PURE__*/(e=>function(t,n,i,a,o){var u=r.I(t);return u&&u.then&&!i?u.then(e.bind(e,t,r.S[t],n,!1,a,o)):e(t,r.S[t],n,i,a,o)})((e,t,r,n,u,p)=>{if(!a(t,r))return d(e,r,p);var c=l(t,r,n);return i(u,c)||f(s(t,r,c,u)),o(t[r][c])}),v={},h={423:()=>c("default","react",!1,[4,18,3,1]),2485:()=>c("default","@activeviam/atoti-ui-sdk",!1,[4,5,2,2]),3145:()=>c("default","antd",!1,[4,5,6,4]),4352:()=>c("default","react-dom",!1,[4,18,3,1]),4457:()=>c("default","react-intl",!1,[4,6,2,5]),4863:()=>c("default","dayjs",!1,[4,1,11,11]),5055:()=>c("default","react-router-dom",!1,[4,6,22,2]),5505:()=>c("default","@activeviam/theme",!1,[4,5,2,2]),5904:()=>c("default","@emotion/react",!1,[4,11,11,4]),6695:()=>c("default","react-dnd",!1,[4,16,0,1]),7179:()=>c("default","@emotion/react/jsx-runtime",!1,[4,11,11,4]),9450:()=>c("default","react-redux",!1,[4,8,0,5])};[423,2485,3145,4352,4457,4863,5055,5505,5904,6695,7179,9450].forEach(e=>{r.m[e]=t=>{v[e]=0,delete r.c[e];var n=h[e]();if("function"!=typeof n)throw Error("Shared module is not available for eager consumption: "+e);t.exports=n()}});var m={356:[423,2485,3145,4352,4457,4863,5055,5505,5904,6695,7179,9450]},g={};r.f.consumes=(e,t)=>{r.o(m,e)&&m[e].forEach(e=>{if(r.o(v,e))return t.push(v[e]);if(!g[e]){var n=t=>{v[e]=0,r.m[e]=n=>{delete r.c[e],n.exports=t()}};g[e]=!0;var i=t=>{delete v[e],r.m[e]=n=>{throw delete r.c[e],t}};try{var a=h[e]();a.then?t.push(v[e]=a.then(n).catch(i)):n(a)}catch(e){i(e)}}})}})(),(()=>{r.b=document.baseURI||self.location.href;var e={57:0,477:0,356:0};r.f.j=(t,n)=>{var i=r.o(e,t)?e[t]:void 0;if(0!==i){if(i)n.push(i[2]);else if(356!=t){var a=new Promise((r,n)=>i=e[t]=[r,n]);n.push(i[2]=a);var o=r.p+r.u(t),u=Error();r.l(o,n=>{if(r.o(e,t)&&(0!==(i=e[t])&&(e[t]=void 0),i)){var a=n&&("load"===n.type?"missing":n.type),o=n&&n.target&&n.target.src;u.message="Loading chunk "+t+" failed.\n("+a+": "+o+")",u.name="ChunkLoadError",u.type=a,u.request=o,i[1](u)}},"chunk-"+t,t)}else e[t]=0}},r.O.j=t=>0===e[t];var t=(t,n)=>{var i,a,[o,u,l]=n,s=0;if(o.some(t=>0!==e[t])){for(i in u)r.o(u,i)&&(r.m[i]=u[i]);if(l)var f=l(r)}for(t&&t(n);s<o.length;s++)a=o[s],r.o(e,a)&&e[a]&&e[a][0](),e[a]=0;return r.O(f)},n=self.webpackChunk_activeviam_limits_ui=self.webpackChunk_activeviam_limits_ui||[];n.forEach(t.bind(null,0)),n.push=t.bind(null,n.push.bind(n))})();var n=r.O(void 0,[980,356],()=>r(1477));n=r.O(n)})();