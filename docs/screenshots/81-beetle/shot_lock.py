import asyncio,sys
from playwright.async_api import async_playwright
port,tag=sys.argv[1],sys.argv[2]
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
            await pg.screenshot(path=f"{tag}_{shot.replace('&specimenCritter=','_')}.png"); await pg.close()
        await b.close()
asyncio.run(main())
