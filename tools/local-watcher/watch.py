"""
Watches a local folder and reports each new or changed file's SHA-256
hash to Spring Boot the moment it appears, before any upload happens.
This gives the server an independent, earlier fingerprint that a later
pre-upload edit cannot retroactively erase, once the hash has left
this machine, the watcher itself no longer matters, killing it or
editing the file afterward changes nothing about what was already
reported.

Usage:
    pip install watchdog requests
    python watch.py /path/to/watched/folder
"""

import hashlib
import sys
import time

import requests
from watchdog.events import FileSystemEventHandler
from watchdog.observers import Observer

SPRING_BOOT_URL = "http://localhost:8080/api/documents/candidate-hash"


def sha256_of_file(path):
    hasher = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


class IntakeHandler(FileSystemEventHandler):

    def on_created(self, event):
        if not event.is_directory:
            self.report(event.src_path)

    def on_modified(self, event):
        if not event.is_directory:
            self.report(event.src_path)

    def report(self, path):
        try:
            file_hash = sha256_of_file(path)
        except OSError:
            return  # file still being written, or already gone

        filename = path.split("/")[-1]

        try:
            requests.post(
                SPRING_BOOT_URL,
                json={"filename": filename, "sha256Hash": file_hash},
                timeout=3,
            )
            print(f"Reported {filename}: {file_hash[:12]}...")
        except requests.RequestException as e:
            print(f"Could not report {filename}: {e}")


if __name__ == "__main__":
    watched_path = sys.argv[1] if len(sys.argv) > 1 else "."
    handler = IntakeHandler()
    observer = Observer()
    observer.schedule(handler, watched_path, recursive=False)
    observer.start()
    print(f"Watching {watched_path} ...")

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()