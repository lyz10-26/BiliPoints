// Exercises the exact WebView probe used by the app with media/DOM fixtures.
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync('BiliPoints/app/src/main/java/cn/pi/bilipoints/HiddenWatch.java', 'utf8');
const script = JSON.parse(source.match(/static final String SAMPLE=("(?:[^"\\]|\\.)*");/)[1]);
function doc(videos, frames = []) { return {querySelectorAll: kind => kind === 'video' ? videos : frames}; }
function video(duration, played = 0) { return {duration, paused:false, ended:false, seeking:false, readyState:4, currentSrc:'episode', played:{length:1,start:()=>0,end:()=>played}}; }
function probe(document) { return JSON.parse(vm.runInNewContext(script, {document, Date, isFinite})); }
(async () => {
 assert.equal(probe(doc([])).found,false);
 const ad=video(30,5), main=video(1500,70);
 assert.equal(probe(doc([ad,main])).played,70,'main episode wins over short ad');
 assert.equal(main.muted,true);
 assert.equal(probe(doc([], [{contentDocument:doc([main])}])).played,70);
 const broken={get contentDocument(){throw new Error('cross-origin');}};
 assert.equal(probe(doc([], [broken])).found,false,'cross-origin iframe remains isolated');
 main.error={code:4};assert.equal(probe(doc([main])).mediaError,4);
 main.error=null;main.paused=true;let attempts=0;
 main.play=()=>{attempts++;return Promise.reject({name:'NotAllowedError'});};
 probe(doc([main]));await Promise.resolve();
 assert.equal(probe(doc([main])).playError,'NotAllowedError');
 assert.equal(attempts,1,'retry attempts are throttled');
 console.log('PASS: live probe selection, frames, media errors and autoplay rejection');
})().catch(e=>{console.error(e);process.exitCode=1;});
