#!/usr/bin/env python3
"""Renders the launcher icons.

The project has no image assets checked in as binaries-by-hand: the PNG
fallbacks used below API 26 are generated here, and the adaptive icon for API 26+
is written as a vector so it stays sharp at every density. Re-run after changing
the geometry or colours below:

    python3 tools/make_icons.py
"""

import os
import struct
import zlib

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "app", "src", "main", "res")

# Density buckets Android looks in for a legacy launcher icon.
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

SAMPLES = 4  # supersampling factor per axis, for smooth edges

BLUE_TOP = (0x4C, 0x8A, 0xFF)
BLUE_BOTTOM = (0x2E, 0x63, 0xE8)
WHITE = (0xFF, 0xFF, 0xFF)

# Geometry in unit coordinates (0..1 across the icon).
BODY = (0.22, 0.155, 0.78, 0.845)   # calculator body
BODY_RADIUS = 0.085
SCREEN = (0.305, 0.245, 0.695, 0.375)
SCREEN_RADIUS = 0.025
DOT_RADIUS = 0.043
DOT_COLUMNS = (0.363, 0.5, 0.637)
DOT_ROWS = (0.505, 0.625, 0.745)


def rounded_rect_contains(x, y, rect, radius):
    x0, y0, x1, y1 = rect
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    # Inside the straight-edged cross; only the corner squares need a radius test.
    cx = min(max(x, x0 + radius), x1 - radius)
    cy = min(max(y, y0 + radius), y1 - radius)
    dx, dy = x - cx, y - cy
    return dx * dx + dy * dy <= radius * radius


def circle_contains(x, y, cx, cy, radius):
    dx, dy = x - cx, y - cy
    return dx * dx + dy * dy <= radius * radius


def background_colour(y):
    """Vertical gradient across the icon's backplate."""
    t = min(max(y, 0.0), 1.0)
    return tuple(int(round(BLUE_TOP[i] + (BLUE_BOTTOM[i] - BLUE_TOP[i]) * t))
                 for i in range(3))


def sample(x, y, round_icon):
    """Colour at a point, or None where the icon is transparent."""
    if round_icon:
        inside_plate = circle_contains(x, y, 0.5, 0.5, 0.5)
    else:
        inside_plate = rounded_rect_contains(x, y, (0.0, 0.0, 1.0, 1.0), 0.22)
    if not inside_plate:
        return None

    if rounded_rect_contains(x, y, BODY, BODY_RADIUS):
        # The screen and the keys are punched back out to the plate colour.
        if rounded_rect_contains(x, y, SCREEN, SCREEN_RADIUS):
            return background_colour(y)
        for row in DOT_ROWS:
            for column in DOT_COLUMNS:
                if circle_contains(x, y, column, row, DOT_RADIUS):
                    return background_colour(y)
        return WHITE
    return background_colour(y)


def render(size, round_icon):
    rows = []
    step = 1.0 / (size * SAMPLES)
    for py in range(size):
        row = bytearray()
        for px in range(size):
            r = g = b = a = 0
            for sy in range(SAMPLES):
                y = (py * SAMPLES + sy + 0.5) * step
                for sx in range(SAMPLES):
                    x = (px * SAMPLES + sx + 0.5) * step
                    colour = sample(x, y, round_icon)
                    if colour is not None:
                        r += colour[0]
                        g += colour[1]
                        b += colour[2]
                        a += 255
            total = SAMPLES * SAMPLES
            if a == 0:
                row += b"\x00\x00\x00\x00"
                continue
            covered = a / 255.0  # number of samples that landed on the icon
            row += bytes((int(round(r / covered)), int(round(g / covered)),
                          int(round(b / covered)), int(round(a / total))))
        rows.append(bytes(row))
    return rows


def write_png(path, rows, size):
    raw = b"".join(b"\x00" + row for row in rows)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    with open(path, "wb") as handle:
        handle.write(png)


def unit_to_viewport(value):
    """Maps unit coordinates into the 72dp safe zone of a 108dp adaptive icon."""
    return 18.0 + 72.0 * value


def rounded_rect_path(rect, radius):
    x0, y0, x1, y1 = (unit_to_viewport(v) for v in rect)
    r = 72.0 * radius
    return (
        "M{:.2f},{:.2f} H{:.2f} A{:.2f},{:.2f} 0 0 1 {:.2f},{:.2f} "
        "V{:.2f} A{:.2f},{:.2f} 0 0 1 {:.2f},{:.2f} H{:.2f} "
        "A{:.2f},{:.2f} 0 0 1 {:.2f},{:.2f} V{:.2f} "
        "A{:.2f},{:.2f} 0 0 1 {:.2f},{:.2f} Z"
    ).format(x0 + r, y0, x1 - r, r, r, x1, y0 + r,
             y1 - r, r, r, x1 - r, y1, x0 + r,
             r, r, x0, y1 - r, y0 + r,
             r, r, x0 + r, y0)


def circle_path(cx, cy, radius):
    x, y = unit_to_viewport(cx), unit_to_viewport(cy)
    r = 72.0 * radius
    return ("M{:.2f},{:.2f} m-{:.2f},0 a{:.2f},{:.2f} 0 1 0 {:.2f},0 "
            "a{:.2f},{:.2f} 0 1 0 -{:.2f},0 Z").format(x, y, r, r, r, 2 * r, r, r, 2 * r)


def write_vector_foreground(path):
    holes = [rounded_rect_path(SCREEN, SCREEN_RADIUS)]
    for row in DOT_ROWS:
        for column in DOT_COLUMNS:
            holes.append(circle_path(column, row, DOT_RADIUS))
    # evenOdd fill turns the screen and keys into holes in the white body.
    data = rounded_rect_path(BODY, BODY_RADIUS) + " " + " ".join(holes)
    xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:fillType="evenOdd"
        android:pathData="{}" />
</vector>
""".format(data)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(xml)


def main():
    for bucket, size in DENSITIES.items():
        directory = os.path.join(RES, "mipmap-" + bucket)
        os.makedirs(directory, exist_ok=True)
        for name, round_icon in (("ic_launcher", False), ("ic_launcher_round", True)):
            write_png(os.path.join(directory, name + ".png"), render(size, round_icon), size)
            print("wrote mipmap-{}/{}.png ({}px)".format(bucket, name, size))

    write_vector_foreground(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"))
    print("wrote drawable/ic_launcher_foreground.xml")


if __name__ == "__main__":
    main()
