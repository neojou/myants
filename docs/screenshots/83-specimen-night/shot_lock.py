# 用法：python3 shot_lock.py PORT TAG SHOTS [WAIT_MS]
# 跟 68-lighting/shot_lock.py 一樣：假時鐘跑 10 秒後 resume。WAIT_MS 是 resume 後再讓真時間走多久才截圖（預設 0），
# 用來證明畫面跟真時間無關（#83）。
import asyncio,sys
from playwright.async_api import async_playwright
port,tag=sys.argv[1],sys.argv[2]
wait=int(sys.argv[4]) if len(sys.argv)>4 else 0
async def main():
    async with async_playwright() as p:
        b=await p.chromium.launch(executable_path="/usr/bin/google-chrome",args=["--use-angle=swiftshader","--enable-unsafe-swiftshader","--ignore-gpu-blocklist","--disable-webgpu"])
        w=await b.new_page(); await w.goto(f"http://localhost:{port}/index.html?backend=gl"); await w.wait_for_timeout(15000); await w.close()
        for shot in sys.argv[3].split(","):
            pg=await b.new_page(viewport={"width":1600,"height":900})
            await pg.clock.install(time="2026-10-26T12:00:00+08:00")
            await pg.goto(f"http://localhost:{port}/index.html?backend=gl&seed=12345&quality=medium&shot={shot}")
            pg.set_default_timeout(0)
            await pg.clock.run_for(10000)
            await pg.clock.resume()
            if wait: await pg.wait_for_timeout(wait)
            name=shot.replace('&specimenCritter=','_')
            await pg.screenshot(path=f"{tag}_{name}.png"); await pg.close()
        await b.close()
asyncio.run(main())
