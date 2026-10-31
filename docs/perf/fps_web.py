import asyncio,sys,json,statistics
from playwright.async_api import async_playwright
port,tag=sys.argv[1],sys.argv[2]
JS="""window.__ft=[];(function f(t){window.__ft.push(t);requestAnimationFrame(f)})(performance.now());"""
async def main():
    async with async_playwright() as p:
        b=await p.chromium.launch(executable_path="/usr/bin/google-chrome",args=["--use-angle=swiftshader","--enable-unsafe-swiftshader","--ignore-gpu-blocklist","--disable-webgpu"])
        pg=await b.new_page(viewport={"width":1600,"height":900})
        logs=[]; pg.on("console",lambda m:logs.append(m.text))
        await pg.goto(f"http://localhost:{port}/index.html?backend=gl&seed=12345&quality=medium")
        await pg.evaluate(JS); await pg.wait_for_timeout(10000)
        await pg.evaluate("window.__ft=[]"); await pg.wait_for_timeout(60000)
        ts=await pg.evaluate("window.__ft")
        info=await pg.evaluate("(()=>{const c=document.createElement('canvas').getContext('webgl2');const e=c.getExtension('WEBGL_debug_renderer_info');return {ua:navigator.userAgent,gl:e?c.getParameter(e.UNMASKED_RENDERER_WEBGL):null}})()")
        await pg.screenshot(path=f"{tag}.png"); await b.close()
    dts=[ (b-a) for a,b in zip(ts,ts[1:])]
    span=(ts[-1]-ts[0])/1000; mean=len(dts)/span
    s=sorted(dts,reverse=True); k=max(1,len(s)//100); low=1000/(sum(s[:k])/k)
    r=dict(tag=tag,frames=len(dts),span_s=round(span,2),mean_fps=round(mean,2),low1_fps=round(low,2),
      median_ms=round(statistics.median(dts),1),max_ms=round(max(dts),1),info=info,
      log=[l for l in logs if any(w in l.lower() for w in ("quality","downgrad","warn","error"))][:20])
    json.dump(dict(r,dts=dts),open(f"{tag}.json","w")); print(json.dumps(r,ensure_ascii=False))
asyncio.run(main())
