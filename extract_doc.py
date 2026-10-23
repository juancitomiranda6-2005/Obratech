import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

src = Path(r'c:/Users/LENOVO/Documents/Obratech/ObraTech-Mejorado/Diseno_Plataforma_Construccion.docx')
if not src.exists():
    raise SystemExit(f'Missing {src}')

with zipfile.ZipFile(src) as z:
    doc = [n for n in z.namelist() if n.endswith('document.xml')][0]
    root = ET.fromstring(z.read(doc))
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    paras = []
    for para in root.findall('.//w:p', ns):
        text = ''.join(node.text or '' for node in para.findall('.//w:t', ns))
        if text.strip():
            paras.append(text)
    text = '\n'.join(paras)
    print(text[:20000])
