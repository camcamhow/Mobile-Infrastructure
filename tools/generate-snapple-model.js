const fs = require("node:fs");
const path = require("node:path");

const root = path.join(__dirname, "..", "src", "main", "resources", "assets", "mobilecellnetwork");
const target = path.join(root, "models", "item");
const lines = ["# Original Snapple phone mesh", "mtllib snapple_realistic.mtl"];
let count = 0;
const bodyHeight = 0.90;
const bodyWidth = bodyHeight * 71.6 / 147.6;
const xScale = bodyWidth / 0.46;
const zScale = (bodyHeight * 7.8 / 147.6) / 0.068;

function face(material, vertices) {
    lines.push(`usemtl ${material}`);
    const indices = vertices.map(([x, y, z, u, v]) => {
        const scaledX = 0.5 + (x - 0.5) * xScale;
        const scaledZ = 0.5 + (z - 0.5) * zScale;
        lines.push(`v ${scaledX.toFixed(6)} ${y.toFixed(6)} ${scaledZ.toFixed(6)}`);
        lines.push(`vt ${u.toFixed(6)} ${v.toFixed(6)}`);
        count++;
        return `${count}/${count}`;
    });
    lines.push(`f ${indices.join(" ")}`);
}

function roundedRect(cx, cy, width, height, radius, z) {
    const points = [];
    for (const [x, y, start] of [
        [cx + width / 2 - radius, cy + height / 2 - radius, 0],
        [cx - width / 2 + radius, cy + height / 2 - radius, 90],
        [cx - width / 2 + radius, cy - height / 2 + radius, 180],
        [cx + width / 2 - radius, cy - height / 2 + radius, 270]
    ]) {
        for (let i = 0; i <= 8; i++) {
            const a = (start + i * 90 / 8) * Math.PI / 180;
            points.push([x + radius * Math.cos(a), y + radius * Math.sin(a), z]);
        }
    }
    return points;
}

function uv(point, cx, cy, width, height) {
    return [(point[0] - (cx - width / 2)) / width, (point[1] - (cy - height / 2)) / height];
}

function surface(points, material, front, cx, cy, width, height) {
    const center = [cx, cy, points[0][2], 0.5, 0.5];
    for (let i = 0; i < points.length; i++) {
        const a = points[i];
        const b = points[(i + 1) % points.length];
        const vertex = p => [...p, ...uv(p, cx, cy, width, height)];
        face(material, front ? [center, vertex(b), vertex(a)] : [center, vertex(a), vertex(b)]);
    }
}

function connect(a, b, material) {
    for (let i = 0; i < a.length; i++) {
        const j = (i + 1) % a.length;
        face(material, [
            [...a[i], 0, 0], [...a[j], 1, 0],
            [...b[j], 1, 1], [...b[i], 0, 1]
        ]);
    }
}

function roundedPlate(cx, cy, width, height, radius, frontZ, backZ, side, front, back) {
    const frontPoints = roundedRect(cx, cy, width, height, radius, frontZ);
    const backPoints = roundedRect(cx, cy, width, height, radius, backZ);
    connect(frontPoints, backPoints, side);
    surface(frontPoints, front, true, cx, cy, width, height);
    surface(backPoints, back, false, cx, cy, width, height);
}

function circle(cx, cy, radius, z, material) {
    const points = Array.from({ length: 32 }, (_, i) => {
        const angle = 2 * Math.PI * i / 32;
        return [cx + radius * Math.cos(angle), cy + radius * Math.sin(angle), z];
    });
    surface(points, material, false, cx, cy, radius * 2, radius * 2);
    return points;
}

const cx = 0.5;
const cy = 0.5;
const width = 0.46;
const height = 0.90;
const radius = 0.064;
const frontEdge = roundedRect(cx, cy, width - 0.012, height - 0.012, radius, 0.466);
const frontBand = roundedRect(cx, cy, width, height, radius, 0.473);
const backBand = roundedRect(cx, cy, width, height, radius, 0.527);
const backEdge = roundedRect(cx, cy, width - 0.012, height - 0.012, radius, 0.534);
connect(frontEdge, frontBand, "rim");
connect(frontBand, backBand, "metal");
connect(backBand, backEdge, "rim");
surface(frontEdge, "glass", true, cx, cy, width, height);
surface(backEdge, "back", false, cx, cy, width, height);

const displayDiagonal = bodyHeight * (6.1 * 25.4) / 147.6;
const displayHeight = displayDiagonal * 19.5 / Math.hypot(19.5, 9);
const displayWidth = displayDiagonal * 9 / Math.hypot(19.5, 9) / xScale;
const screen = roundedRect(cx, cy, displayWidth, displayHeight, radius - 0.009, 0.4655);
surface(screen, "screen", true, cx, cy, displayWidth, displayHeight);

roundedPlate(0.352, 0.782, 0.117, 0.252, 0.055, 0.535, 0.548, "metal", "back", "camera_base");
for (const y of [0.838, 0.727]) {
    const bottom = circle(0.352, y, 0.052, 0.548, "metal");
    const top = circle(0.352, y, 0.052, 0.562, "rim");
    connect(bottom, top, "metal");
    circle(0.352, y, 0.043, 0.5625, "lens");
    circle(0.352, y, 0.027, 0.563, "lens_inner");
    circle(0.343, y + 0.009, 0.008, 0.5635, "highlight");
}
circle(0.430, 0.841, 0.014, 0.535, "flash");
circle(0.430, 0.800, 0.005, 0.535, "glass");

roundedPlate(0.267, 0.696, 0.010, 0.075, 0.004, 0.487, 0.515, "rim", "metal", "metal");
roundedPlate(0.267, 0.594, 0.010, 0.075, 0.004, 0.487, 0.515, "rim", "metal", "metal");
roundedPlate(0.733, 0.655, 0.010, 0.110, 0.004, 0.487, 0.515, "rim", "metal", "metal");

fs.writeFileSync(path.join(target, "snapple_realistic.obj"), lines.join("\n") + "\n");
