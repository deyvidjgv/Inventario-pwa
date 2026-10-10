from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
import os

APK_PATH = "/home/Seveck/Inventario-pwa/app/build/outputs/apk/debug/app-debug.apk"

class ApkHandler(BaseHTTPRequestHandler):
    def do_HEAD(self):
        if not os.path.exists(APK_PATH):
            self.send_error(404, "APK not found")
            return
        file_size = os.path.getsize(APK_PATH)
        self.send_response(200)
        self.send_header("Content-Type", "application/vnd.android.package-archive")
        self.send_header("Content-Disposition", 'attachment; filename="InventarioPool.apk"')
        self.send_header("Content-Length", str(file_size))
        self.send_header("Connection", "close")
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        self.end_headers()

    def do_GET(self):
        if not os.path.exists(APK_PATH):
            self.send_error(404, "APK not found")
            return
        
        file_size = os.path.getsize(APK_PATH)
        self.send_response(200)
        self.send_header("Content-Type", "application/vnd.android.package-archive")
        self.send_header("Content-Disposition", 'attachment; filename="InventarioPool.apk"')
        self.send_header("Content-Length", str(file_size))
        self.send_header("Connection", "close")
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        self.end_headers()
        
        with open(APK_PATH, "rb") as f:
            while chunk := f.read(65536):
                try:
                    self.wfile.write(chunk)
                except BrokenPipeError:
                    break

if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8080), ApkHandler)
    print("Serving APK on 0.0.0.0:8080 (multithreaded)...")
    server.serve_forever()
