import os
import shutil
import hashlib
import traceback

from base_plugin import BasePlugin
from android_utils import log
from org.telegram.messenger import ApplicationLoader
from dalvik.system import DexClassLoader
from java.io import File
from java.util import HashMap
from java.lang import Object
from java import jclass

try:
    from elyx import mvel_execute
except Exception:
    mvel_execute = None

try:
    from org.mvel2 import MVEL
except Exception:
    MVEL = None


def execute_java_script(script: str, variables: dict):
    if mvel_execute:
        try:
            return mvel_execute(script, variables)
        except Exception as e:
            log(f"[LiquidGlass] mvel_execute notice: {e}, fallback to MVEL.eval")
    if MVEL:
        jmap = HashMap()
        for k, v in variables.items():
            jmap.put(k, v)
        return MVEL.eval(script, jmap)
    raise RuntimeError("Neither elyx.mvel_execute nor org.mvel2.MVEL is available")


class LiquidGlassPlugin(BasePlugin):
    def __init__(self):
        super().__init__()
        self.ctx = None
        self.java_holder = HashMap()

    def on_plugin_load(self):
        try:
            self.ctx = ApplicationLoader.applicationContext
            if not self.ctx:
                log("[LiquidGlass] ApplicationContext is null, aborting")
                return

            self._do_unload()

            bundled_dex = None
            try:
                from elyx import assets
                asset = assets.get("dex/classes.dex")
                if asset and hasattr(asset, "path"):
                    bundled_dex = str(asset.path)
            except Exception as e:
                log(f"[LiquidGlass] assets.get notice: {e}")

            if not bundled_dex or not os.path.exists(bundled_dex):
                for c in [
                    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "res", "dex", "classes.dex")),
                    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "res", "classes.dex")),
                    os.path.abspath(os.path.join(os.path.dirname(__file__), "classes.dex")),
                ]:
                    if os.path.exists(c):
                        bundled_dex = c
                        break

            if not bundled_dex or not os.path.exists(bundled_dex):
                raise FileNotFoundError(f"classes.dex not found near {__file__}")

            log(f"[LiquidGlass] Found bundled DEX: {bundled_dex}")

            dest_dir = File(self.ctx.getFilesDir(), "liquidglass_plugin")
            if not dest_dir.exists():
                dest_dir.mkdirs()
            dest_file = File(dest_dir, "classes.dex")
            dest_path = dest_file.getAbsolutePath()

            def md5(p):
                try:
                    with open(p, "rb") as f:
                        return hashlib.md5(f.read()).hexdigest()
                except Exception:
                    return ""

            if not dest_file.exists() or md5(bundled_dex) != md5(dest_path):
                if dest_file.exists():
                    try:
                        dest_file.setWritable(True, True)
                        dest_file.delete()
                    except Exception:
                        pass
                shutil.copyfile(bundled_dex, dest_path)
                log(f"[LiquidGlass] Deployed fresh DEX -> {dest_path}")

            try:
                dest_file.setReadOnly()
            except Exception:
                pass

            opt_dir = self.ctx.getDir("dex_opt", 0).getAbsolutePath()
            loader = DexClassLoader(dest_path, opt_dir, None, self.ctx.getClassLoader())

            vars = {
                "loader": loader,
                "ctx": self.ctx,
                "holder": self.java_holder,
                "object_cls": jclass("java.lang.Object"),
            }
            load_script = """
Class cls = loader.loadClass("com.iosglass.LiquidGlassBridge");
Object inst = cls.newInstance();
cls.getMethod("onLoad", object_cls, object_cls).invoke(inst, ctx, null);
holder.put("instance", inst);
return "SUCCESS";
"""
            res = execute_java_script(load_script, vars)
            log(f"[LiquidGlass] Loaded: {res}")

        except Exception as e:
            err = traceback.format_exc()
            log(f"[LiquidGlass] Critical error in on_plugin_load:\n{err}")
            raise e

    def on_plugin_unload(self):
        self._do_unload()

    def _do_unload(self):
        try:
            if self.java_holder and self.java_holder.containsKey("instance"):
                script = """
Object inst = holder.get("instance");
if (inst != null) {
    inst.onUnload();
    holder.clear();
}
return "SUCCESS";
"""
                execute_java_script(script, {"holder": self.java_holder})
                log("[LiquidGlass] Unloaded.")
        except Exception as e:
            log(f"[LiquidGlass] Error in unload: {e}")
