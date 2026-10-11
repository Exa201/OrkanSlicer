from pathlib import Path
import json
import zipfile

dest = Path(__file__).resolve().parents[1] / "app/src/androidTest/assets/models/two-color-project.3mf"
vertices = [(0,0,0),(6,0,0),(6,6,0),(0,6,0),(0,0,6),(6,0,6),(6,6,6),(0,6,6)]
faces = [(0,2,1),(0,3,2),(4,5,6),(4,6,7),(0,1,5),(0,5,4),(1,2,6),(1,6,5),(2,3,7),(2,7,6),(3,0,4),(3,4,7)]
mesh = '<mesh><vertices>' + ''.join(f'<vertex x="{x}" y="{y}" z="{z}"/>' for x,y,z in vertices)
mesh += '</vertices><triangles>' + ''.join(f'<triangle v1="{a}" v2="{b}" v3="{c}"/>' for a,b,c in faces) + '</triangles></mesh>'
model = '<?xml version="1.0" encoding="UTF-8"?><model unit="millimeter" xmlns="http://schemas.microsoft.com/3dmanufacturing/core/2015/02" xmlns:slic3rpe="http://schemas.slic3r.org/3mf/2017/06"><metadata name="slic3rpe:Version3mf">1</metadata><resources>'
model += ''.join(f'<object id="{i}" name="Testwuerfel {i}" type="model">{mesh}</object>' for i in (1,2))
model += '</resources><build><item objectid="1" transform="1 0 0 0 1 0 0 0 1 40 40 0"/><item objectid="2" transform="1 0 0 0 1 0 0 0 1 55 40 0"/></build></model>'
settings = dict(layer_height="0.24",initial_layer_print_height="0.24",wall_loops="3",sparse_infill_density="25%",
    filament_colour=["#112233","#FFEEDD"],filament_type=["PLA","PETG"],nozzle_temperature=["211","236"],
    nozzle_temperature_initial_layer=["212","237"],hot_plate_temp=["61","72"],hot_plate_temp_initial_layer=["62","73"],
    enable_pressure_advance=[True,True],pressure_advance=["0.04","0.06"],enable_prime_tower=False,
    print_host="ignored-project-host.invalid",z_offset="5",nozzle_diameter=["0.8"],machine_start_gcode="IGNORED")
with zipfile.ZipFile(dest,"w",zipfile.ZIP_DEFLATED) as z:
    z.writestr('[Content_Types].xml','<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/></Types>')
    z.writestr('_rels/.rels','<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Target="/3D/3dmodel.model" Id="rel0" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/></Relationships>')
    z.writestr('3D/3dmodel.model',model)
    z.writestr('Metadata/model_settings.config','<config>' + ''.join(f'<object id="{i}"><metadata key="name" value="Testwuerfel {i}"/><metadata key="extruder" value="{i}"/></object>' for i in (1,2)) + '</config>')
    z.writestr('Metadata/project_settings.config', json.dumps(settings))
print(f'{dest.name}: {dest.stat().st_size} bytes, 2 cubes, 2 filaments')
