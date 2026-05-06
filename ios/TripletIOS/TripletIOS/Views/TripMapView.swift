import MapKit
import SwiftUI

struct TripMapView: View {
    var expenses: [Expense]
    var samples: [LocationSample]
    var height: CGFloat

    @State private var position: MapCameraPosition = .region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 37.575, longitude: 126.990),
            span: MKCoordinateSpan(latitudeDelta: 0.025, longitudeDelta: 0.025)
        )
    )
    @State private var selectedExpenseID: Expense.ID?

    private var expensesWithCoordinates: [Expense] {
        expenses
            .filter { $0.coordinate != nil }
            .sorted { $0.occurredAt < $1.occurredAt }
    }

    private var routeCoordinates: [CLLocationCoordinate2D] {
        let expenseCoordinates = expensesWithCoordinates.compactMap(\.coordinate)
        if expenseCoordinates.count >= 2 {
            return expenseCoordinates
        }
        return samples.sorted { $0.capturedAt < $1.capturedAt }.map(\.coordinate)
    }

    private var allCoordinates: [CLLocationCoordinate2D] {
        routeCoordinates + expensesWithCoordinates.compactMap(\.coordinate)
    }

    private var selectedExpense: Expense? {
        guard let selectedExpenseID else { return nil }
        return expenses.first { $0.id == selectedExpenseID }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Map(position: $position, selection: $selectedExpenseID) {
                if routeCoordinates.count >= 2 {
                    MapPolyline(coordinates: routeCoordinates)
                        .stroke(TripletColor.blue, lineWidth: 6)
                }
                ForEach(expensesWithCoordinates) { expense in
                    if let coordinate = expense.coordinate {
                        Marker(expense.merchantName, coordinate: coordinate)
                            .tint(expense.id == selectedExpenseID ? TripletColor.red : TripletColor.orange)
                            .tag(expense.id)
                    }
                }
            }
            .mapStyle(.standard(elevation: .flat))
            .frame(height: height)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .onAppear {
                moveCameraToContent()
            }
            .onChange(of: expenses) { _, _ in
                moveCameraToContent()
            }

            if allCoordinates.isEmpty {
                VStack {
                    HStack {
                        Label("결제 알림을 보내면 이 지도에 핀이 표시됩니다.", systemImage: "mappin.and.ellipse")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(TripletColor.grey700)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 9)
                            .background(.white.opacity(0.92))
                            .clipShape(Capsule())
                            .shadow(color: .black.opacity(0.08), radius: 8, x: 0, y: 4)
                        Spacer()
                    }
                    Spacer()
                }
                .padding(12)
            }

            if let selectedExpense {
                MapSelectionCard(expense: selectedExpense)
                    .padding(12)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.28, dampingFraction: 0.9), value: selectedExpenseID)
    }

    private func moveCameraToContent() {
        let coordinates = allCoordinates
        guard !coordinates.isEmpty else { return }
        position = .region(region(for: coordinates))
    }

    private func region(for coordinates: [CLLocationCoordinate2D]) -> MKCoordinateRegion {
        guard let first = coordinates.first else {
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 37.575, longitude: 126.990),
                span: MKCoordinateSpan(latitudeDelta: 0.025, longitudeDelta: 0.025)
            )
        }
        let minLat = coordinates.map(\.latitude).min() ?? first.latitude
        let maxLat = coordinates.map(\.latitude).max() ?? first.latitude
        let minLng = coordinates.map(\.longitude).min() ?? first.longitude
        let maxLng = coordinates.map(\.longitude).max() ?? first.longitude
        let center = CLLocationCoordinate2D(
            latitude: (minLat + maxLat) / 2,
            longitude: (minLng + maxLng) / 2
        )
        let span = MKCoordinateSpan(
            latitudeDelta: max((maxLat - minLat) * 1.8, 0.012),
            longitudeDelta: max((maxLng - minLng) * 1.8, 0.012)
        )
        return MKCoordinateRegion(center: center, span: span)
    }
}

struct MapSelectionCard: View {
    var expense: Expense

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text(expense.merchantName)
                    .font(.headline.weight(.bold))
                    .lineLimit(1)
                Text(TripletFormat.displayDate.string(from: expense.occurredAt))
                    .font(.caption)
                    .foregroundStyle(TripletColor.grey600)
            }
            Spacer()
            Text(TripletFormat.won(expense.amountMinor))
                .font(.headline.weight(.black))
        }
        .padding(16)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: .black.opacity(0.12), radius: 16, x: 0, y: 8)
    }
}
