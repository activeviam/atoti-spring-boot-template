(()=>{"use strict";var e={1845:(e,r,t)=>{var a={".":()=>Promise.all([t.e(980),t.e(356),t.e(477)]).then(()=>()=>t(1477))},i=(e,r)=>(t.R=r,r=t.o(a,e)?a[e]():Promise.resolve().then(()=>{throw Error('Module "'+e+'" does not exist in container.')}),t.R=void 0,r),n=(e,r)=>{if(t.S){var a="default",i=t.S[a];if(i&&i!==e)throw Error("Container initialization failed as it has already been initialized with a different share scope");return t.S[a]=e,t.I(a,r)}};t.d(r,{get:()=>i,init:()=>n})}},r={};function t(a){var i=r[a];if(void 0!==i)return i.exports;var n=r[a]={exports:{}};return e[a].call(n.exports,n,n.exports,t),n.exports}t.m=e,t.c=r,t.n=e=>{var r=e&&e.__esModule?()=>e.default:()=>e;return t.d(r,{a:r}),r},t.d=(e,r)=>{for(var a in r)t.o(r,a)&&!t.o(e,a)&&Object.defineProperty(e,a,{enumerable:!0,get:r[a]})},t.f={},t.e=e=>Promise.all(Object.keys(t.f).reduce((r,a)=>(t.f[a](e,r),r),[])),t.u=e=>980===e?"static/js/980.a7d51f0c.js":"static/js/async/"+e+"."+({184:"1211e9dd",349:"9d182ff6",477:"a57189d5"})[e]+".js",t.miniCssF=e=>{},t.g=function(){if("object"==typeof globalThis)return globalThis;try{return this||Function("return this")()}catch(e){if("object"==typeof window)return window}}(),t.o=(e,r)=>Object.prototype.hasOwnProperty.call(e,r),(()=>{var e={},r="@activeviam/limits-ui:";t.l=(a,i,n,o)=>{if(e[a]){e[a].push(i);return}if(void 0!==n)for(var u,l,s=document.getElementsByTagName("script"),f=0;f<s.length;f++){var d=s[f];if(d.getAttribute("src")==a||d.getAttribute("data-webpack")==r+n){u=d;break}}u||(l=!0,(u=document.createElement("script")).charset="utf-8",u.timeout=120,t.nc&&u.setAttribute("nonce",t.nc),u.setAttribute("data-webpack",r+n),u.src=a),e[a]=[i];var c=(r,t)=>{u.onerror=u.onload=null,clearTimeout(p);var i=e[a];if(delete e[a],u.parentNode&&u.parentNode.removeChild(u),i&&i.forEach(e=>e(t)),r)return r(t)},p=setTimeout(c.bind(null,void 0,{type:"timeout",target:u}),12e4);u.onerror=c.bind(null,u.onerror),u.onload=c.bind(null,u.onload),l&&document.head.appendChild(u)}})(),t.r=e=>{"undefined"!=typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(e,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(e,"__esModule",{value:!0})},(()=>{t.S={};var e={},r={};t.I=(a,i)=>{i||(i=[]);var n=r[a];if(n||(n=r[a]={}),!(i.indexOf(n)>=0)){if(i.push(n),e[a])return e[a];t.o(t.S,a)||(t.S[a]={}),t.S[a];var o=[];return o.length?e[a]=Promise.all(o).then(()=>e[a]=1):e[a]=1}}})(),(()=>{t.g.importScripts&&(e=t.g.location+"");var e,r=t.g.document;if(!e&&r&&(r.currentScript&&"SCRIPT"===r.currentScript.tagName.toUpperCase()&&(e=r.currentScript.src),!e)){var a=r.getElementsByTagName("script");if(a.length)for(var i=a.length-1;i>-1&&(!e||!/^http(s?):/.test(e));)e=a[i--].src}if(!e)throw Error("Automatic publicPath is not supported in this browser");e=e.replace(/#.*$/,"").replace(/\?.*$/,"").replace(/\/[^\/]+$/,"/"),t.p=e})(),(()=>{var e=e=>{var r=e=>e.split(".").map(e=>+e==e?+e:e),t=/^([^-+]+)?(?:-([^+]+))?(?:\+(.+))?$/.exec(e),a=t[1]?r(t[1]):[];return t[2]&&(a.length++,a.push.apply(a,r(t[2]))),t[3]&&(a.push([]),a.push.apply(a,r(t[3]))),a},r=(r,t)=>{r=e(r),t=e(t);for(var a=0;;){if(a>=r.length)return a<t.length&&"u"!=(typeof t[a])[0];var i=r[a],n=(typeof i)[0];if(a>=t.length)return"u"==n;var o=t[a],u=(typeof o)[0];if(n!=u)return"o"==n&&"n"==u||"s"==u||"u"==n;if("o"!=n&&"u"!=n&&i!=o)return i<o;a++}},a=e=>{var r=e[0],t="";if(1===e.length)return"*";if(r+.5){t+=0==r?">=":-1==r?"<":1==r?"^":2==r?"~":r>0?"=":"!=";for(var i=1,n=1;n<e.length;n++)i--,t+="u"==(typeof(u=e[n]))[0]?"-":(i>0?".":"")+(i=2,u);return t}var o=[];for(n=1;n<e.length;n++){var u=e[n];o.push(0===u?"not("+l()+")":1===u?"("+l()+" || "+l()+")":2===u?o.pop()+" "+o.pop():a(u))}return l();function l(){return o.pop().replace(/^\((.+)\)$/,"$1")}},i=(r,t)=>{if(0 in r){t=e(t);var a=r[0],n=a<0;n&&(a=-a-1);for(var o=0,u=1,l=!0;;u++,o++){var s,f,d=u<r.length?(typeof r[u])[0]:"";if(o>=t.length||"o"==(f=(typeof(s=t[o]))[0]))return!l||("u"==d?u>a&&!n:""==d!=n);if("u"==f){if(!l||"u"!=d)return!1}else if(l){if(d==f){if(u<=a){if(s!=r[u])return!1}else{if(n?s>r[u]:s<r[u])return!1;s!=r[u]&&(l=!1)}}else if("s"!=d&&"n"!=d){if(n||u<=a)return!1;l=!1,u--}else{if(u<=a||f<d!=n)return!1;l=!1}}else"s"!=d&&"n"!=d&&(l=!1,u--)}}var c=[],p=c.pop.bind(c);for(o=1;o<r.length;o++){var h=r[o];c.push(1==h?p()|p():2==h?p()&p():h?i(h,t):!p())}return!!p()},n=(e,r)=>e&&t.o(e,r),o=e=>(e.loaded=1,e.get()),u=e=>Object.keys(e).reduce((r,t)=>(e[t].eager&&(r[t]=e[t]),r),{}),l=(e,t,a)=>{var i=a?u(e[t]):e[t];return Object.keys(i).reduce((e,t)=>!e||!i[e].loaded&&r(e,t)?t:e,0)},s=(e,r,t,i)=>"Unsatisfied version "+t+" from "+(t&&e[r][t].from)+" of shared singleton module "+r+" (required "+a(i)+")",f=e=>{throw Error(e)},d=(e,r)=>f("Shared module "+r+" doesn't exist in shared scope "+e),c=(e,r,t)=>t?t():d(e,r),p=/*#__PURE__*/(e=>function(r,a,i,n,o){var u=t.I(r);return u&&u.then&&!i?u.then(e.bind(e,r,t.S[r],a,!1,n,o)):e(r,t.S[r],a,i,n,o)})((e,r,t,a,u,d)=>{if(!n(r,t))return c(e,t,d);var p=l(r,t,a);return i(u,p)||f(s(r,t,p,u)),o(r[t][p])}),h={},v={423:()=>p("default","react",!1,[4,18,3,1]),2485:()=>p("default","@activeviam/atoti-ui-sdk",!1,[4,5,2,2]),3145:()=>p("default","antd",!1,[4,5,6,4]),4352:()=>p("default","react-dom",!1,[4,18,3,1]),4457:()=>p("default","react-intl",!1,[4,6,2,5]),4863:()=>p("default","dayjs",!1,[4,1,11,11]),5055:()=>p("default","react-router-dom",!1,[4,6,22,2]),5505:()=>p("default","@activeviam/theme",!1,[4,5,2,2]),5904:()=>p("default","@emotion/react",!1,[4,11,11,4]),6695:()=>p("default","react-dnd",!1,[4,16,0,1]),7179:()=>p("default","@emotion/react/jsx-runtime",!1,[4,11,11,4]),9450:()=>p("default","react-redux",!1,[4,8,0,5])},m={356:[423,2485,3145,4352,4457,4863,5055,5505,5904,6695,7179,9450]},g={};t.f.consumes=(e,r)=>{t.o(m,e)&&m[e].forEach(e=>{if(t.o(h,e))return r.push(h[e]);if(!g[e]){var a=r=>{h[e]=0,t.m[e]=a=>{delete t.c[e],a.exports=r()}};g[e]=!0;var i=r=>{delete h[e],t.m[e]=a=>{throw delete t.c[e],r}};try{var n=v[e]();n.then?r.push(h[e]=n.then(a).catch(i)):a(n)}catch(e){i(e)}}})}})(),(()=>{t.b=document.baseURI||self.location.href;var e={864:0};t.f.j=(r,a)=>{var i=t.o(e,r)?e[r]:void 0;if(0!==i){if(i)a.push(i[2]);else if(356!=r){var n=new Promise((t,a)=>i=e[r]=[t,a]);a.push(i[2]=n);var o=t.p+t.u(r),u=Error();t.l(o,a=>{if(t.o(e,r)&&(0!==(i=e[r])&&(e[r]=void 0),i)){var n=a&&("load"===a.type?"missing":a.type),o=a&&a.target&&a.target.src;u.message="Loading chunk "+r+" failed.\n("+n+": "+o+")",u.name="ChunkLoadError",u.type=n,u.request=o,i[1](u)}},"chunk-"+r,r)}else e[r]=0}};var r=(r,a)=>{var i,n,[o,u,l]=a,s=0;if(o.some(r=>0!==e[r])){for(i in u)t.o(u,i)&&(t.m[i]=u[i]);l&&l(t)}for(r&&r(a);s<o.length;s++)n=o[s],t.o(e,n)&&e[n]&&e[n][0](),e[n]=0},a=self.webpackChunk_activeviam_limits_ui=self.webpackChunk_activeviam_limits_ui||[];a.forEach(r.bind(null,0)),a.push=r.bind(null,a.push.bind(a))})();var a=t(1845);self["@activeviam/limits-ui"]=a})();