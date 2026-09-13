from pathlib import Path
import base64,gzip
root=Path(__file__).resolve().parents[2]
out=root/'design-review/Control-Desk-Design'
model=base64.b64encode(gzip.compress((out/'control-desk.glb').read_bytes())).decode()
html=Path(__file__).with_name('preview-template.html').read_text(encoding='utf-8-sig').replace('__MODEL__',model)
(out/'3D-Preview.html').write_text(html,encoding='utf-8')
