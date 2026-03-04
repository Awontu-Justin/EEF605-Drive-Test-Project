import flet as ft
import flet_map as map
import flet_charts as fcharts
import pyrebase
import asyncio
import warnings
from datetime import datetime

# Suppress internal library warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

# ========================================
# FIREBASE CONFIGURATION
# ========================================
firebase_config = {
    "apiKey": "AIzaSyBp0BZXSNpbq3BDdmyYi4WHth5CXnjG1YM",
    "authDomain": "eef605-drivetest.firebaseapp.com",
    "databaseURL": "https://eef605-drivetest-default-rtdb.firebaseio.com",
    "projectId": "eef605-drivetest",
    "storageBucket": "eef605-drivetest.firebasestorage.app",
    "messagingSenderId": "630766056722",
    "appId": "1:630766056722:web:4531fc240c3d1e7e11afcb"
}

class NetworkQualityAnalyzer:
    @staticmethod
    def get_quality(point):
        raw_rat = point.get("ratType", "UNKNOWN")
        rat = str(raw_rat).upper()
        
        try:
            if any(x in rat for x in ["4G", "LTE", "13", "19", "NR", "5G"]):
                val = float(point.get("rsrp", -140))
                return {"val": val, "tech": "4G", "color": ft.Colors.GREEN_600}
            elif any(x in rat for x in ["3G", "HSPA", "WCDMA", "UMTS", "3", "8", "9", "10", "15"]):
                val = float(point.get("rscp", -120))
                return {"val": val, "tech": "3G", "color": ft.Colors.BLUE_600}
            else:
                val = float(point.get("rxLev", -120))
                return {"val": val, "tech": "2G", "color": ft.Colors.AMBER_700}
        except:
            return {"val": -140.0, "tech": "UNKNOWN", "color": ft.Colors.GREY_400}

class CrowdsensedMonitoringApp:
    def __init__(self, page: ft.Page):
        self.page = page
        self.page.title = "Cameroon Network Monitor"
        self.page.theme_mode = ft.ThemeMode.LIGHT
        self.page.padding = 0

        try:
            self.firebase = pyrebase.initialize_app(firebase_config)
            self.db = self.firebase.database()
        except:
            self.db = None

        self.analyzer = NetworkQualityAnalyzer()
        self.data_points = []
        self.analyzed_cache = [] 
        self.map_markers = [] 

        self.main_container = ft.Container(expand=True)
        self.loader = ft.ProgressBar(width=400, visible=False, color=ft.Colors.BLUE_900)
        self.status_text = ft.Text("Ready", size=12, weight="bold", color=ft.Colors.BLUE_GREY_700)

    async def initialize(self):
        self.setup_ui()
        await self.fetch_data()

    def setup_ui(self):
        self.page.appbar = ft.AppBar(
            title=ft.Text("Drive Test Dashboard", color=ft.Colors.WHITE),
            center_title=True,
            bgcolor=ft.Colors.BLUE_900,
            actions=[ft.IconButton(ft.Icons.SYNC, icon_color=ft.Colors.WHITE, on_click=self.on_refresh_click)]
        )

        self.bottom_nav = ft.NavigationBar(
            selected_index=0,
            destinations=[
                ft.NavigationBarDestination(icon=ft.Icons.DASHBOARD, label="Stats"),
                ft.NavigationBarDestination(icon=ft.Icons.MAP, label="Map"),
                ft.NavigationBarDestination(icon=ft.Icons.TIMELINE, label="Variation")
            ],
            on_change=self.on_nav_change
        )

        self.page.add(
            ft.Column([
                self.loader, 
                ft.Container(self.status_text, padding=ft.padding.only(left=20, top=5))
            ], horizontal_alignment=ft.CrossAxisAlignment.CENTER),
            self.main_container,
            self.bottom_nav
        )

    async def on_refresh_click(self, e):
        await self.fetch_data()

    async def on_nav_change(self, e):
        self.refresh_view()

    async def fetch_data(self):
        if not self.db: 
            self.status_text.value = "Error: Database connection failed."
            self.page.update()
            return

        self.loader.visible = True
        self.status_text.value = "Synchronizing with Firebase 'logs' node..."
        self.page.update()

        try:
            loop = asyncio.get_running_loop()
            results = await loop.run_in_executor(None, 
                lambda: self.db.child("logs").order_by_key().limit_to_last(500).get()
            )
            
            if results and results.val():
                raw_data = results.val()
                if isinstance(raw_data, dict):
                    sorted_keys = sorted(raw_data.keys())
                    fetched = [raw_data[k] for k in sorted_keys]
                else:
                    fetched = raw_data
                
                self.data_points = []
                for p in fetched:
                    if isinstance(p, dict) and p.get("latitude") and p.get("longitude"):
                        ts = p.get("timestamp", p.get("time", 0))
                        try:
                            if ts > 1e11: ts /= 1000 
                            p["date_obj"] = datetime.fromtimestamp(ts)
                            p["date_str"] = p["date_obj"].strftime("%H:%M:%S")
                        except:
                            p["date_str"] = "N/A"
                        self.data_points.append(p)
                
                if self.data_points:
                    self.analyzed_cache = [self.analyzer.get_quality(p) for p in self.data_points]
                    self.map_markers = []
                    step = max(1, len(self.data_points) // 200) 
                    for i in range(0, len(self.data_points), step):
                        p, q = self.data_points[i], self.analyzed_cache[i]
                        self.map_markers.append(
                            map.Marker(
                                content=ft.Icon(ft.Icons.CIRCLE, color=q["color"], size=10),
                                coordinates=map.MapLatitudeLongitude(float(p["latitude"]), float(p["longitude"]))
                            )
                        )
                    self.status_text.value = f"Success: Loaded {len(self.data_points)} points."
                else:
                    self.status_text.value = "Connected, but no valid GPS entries found."
            else:
                self.status_text.value = "The 'logs' node is empty."
        except Exception as e:
            self.status_text.value = f"Data Error: {str(e)}"
        
        self.loader.visible = False
        self.refresh_view()

    def refresh_view(self):
        if not self.data_points:
            self.main_container.content = ft.Column([
                ft.Icon(ft.Icons.STORAGE, size=50, color=ft.Colors.GREY_400),
                ft.Text("Waiting for Data...", size=16, color=ft.Colors.GREY_600)
            ], alignment="center", horizontal_alignment="center")
            self.page.update()
            return

        idx = self.bottom_nav.selected_index
        if idx == 0: self.show_home_screen()
        elif idx == 1: self.show_map_screen()
        elif idx == 2: self.show_variation_screen()

    def show_home_screen(self):
        tech_counts = {"4G": 0, "3G": 0, "2G": 0, "UNKNOWN": 0}
        for item in self.analyzed_cache:
            tech_counts[item["tech"]] += 1

        self.main_container.content = ft.Column([
            ft.Container(height=20),
            ft.Text("Live Network Statistics", size=22, weight="bold"),
            ft.Row([
                self.create_stat_card("4G LTE", str(tech_counts["4G"]), ft.Icons.SPEED, ft.Colors.GREEN_600),
                self.create_stat_card("3G WCDMA", str(tech_counts["3G"]), ft.Icons.NETWORK_CHECK, ft.Colors.BLUE_600),
                self.create_stat_card("2G GSM", str(tech_counts["2G"]), ft.Icons.CELL_TOWER, ft.Colors.AMBER_700),
            ], alignment="center", wrap=True),
        ], horizontal_alignment="center", scroll="auto")
        self.page.update()

    def show_map_screen(self):
        if not self.data_points:
            return
            
        last_p = self.data_points[-1]
        
        self.main_container.content = ft.Container(
            content=map.Map(
                expand=True,
                initial_center=map.MapLatitudeLongitude(float(last_p["latitude"]), float(last_p["longitude"])),
                initial_zoom=12,
                layers=[
                    map.TileLayer(
                        url_template="https://tile.openstreetmap.org/{z}/{x}/{y}.png",
                    ),
                    map.MarkerLayer(markers=self.map_markers)
                ]
            ),
            expand=True
        )
        self.page.update()

    def show_variation_screen(self):
        techs = ["4G", "3G", "2G"]
        colors = [ft.Colors.GREEN_600, ft.Colors.BLUE_600, ft.Colors.AMBER_700]
        series_list = []
        step = max(1, len(self.data_points) // 50)

        for tech, color in zip(techs, colors):
            points = []
            for i in range(0, len(self.data_points), step):
                if self.analyzed_cache[i]["tech"] == tech:
                    points.append(fcharts.LineChartDataPoint(i, self.analyzed_cache[i]["val"]))
            
            if points:
                series_list.append(fcharts.LineChartData(
                    points=points, color=color, stroke_width=3, curved=True,
                    below_line_bgcolor=ft.Colors.with_opacity(0.1, color)
                ))

        self.main_container.content = ft.Container(
            content=ft.Column([
                ft.Text("Signal Strength Variation (dBm)", size=18, weight="bold"),
                ft.Container(
                    fcharts.LineChart(
                        data_series=series_list,
                        expand=True,
                        bottom_axis=fcharts.ChartAxis(
                            title=ft.Text("Time Samples"),
                            labels=[fcharts.ChartAxisLabel(value=i, label=ft.Text(self.data_points[i]["date_str"], size=9, rotate=45)) 
                                   for i in range(0, len(self.data_points), step*8)]
                        ),
                        left_axis=fcharts.ChartAxis(title=ft.Text("Signal Level")),
                    ),
                    height=400
                )
            ]), 
            padding=20
        )
        self.page.update()

    def create_stat_card(self, title, value, icon, color):
        return ft.Container(
            content=ft.Column([
                ft.Icon(icon, color=color, size=30),
                ft.Text(value, weight="bold", size=24),
                ft.Text(title, size=12, color=ft.Colors.GREY_700)
            ], horizontal_alignment="center"),
            padding=15, width=130, border_radius=15, 
            border=ft.Border.all(1, ft.Colors.GREY_300),
            bgcolor=ft.Colors.WHITE
        )

async def main(page: ft.Page):
    app = CrowdsensedMonitoringApp(page)
    await app.initialize()

if __name__ == "__main__":
    ft.app(target=main)