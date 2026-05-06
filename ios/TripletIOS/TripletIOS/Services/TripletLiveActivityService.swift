import ActivityKit
import Foundation

@MainActor
final class TripletLiveActivityService {
    func start(startedAt: Date, state: TripletTravelActivityAttributes.ContentState) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }

        Task {
            await endAll(dismissalPolicy: .immediate, state: state)
            let attributes = TripletTravelActivityAttributes(
                tripName: "이번 여행",
                startedAt: startedAt
            )
            do {
                _ = try Activity.request(
                    attributes: attributes,
                    content: ActivityContent(state: state, staleDate: nil),
                    pushType: nil
                )
            } catch {
                print("Triplet Live Activity start failed: \(error.localizedDescription)")
            }
        }
    }

    func update(state: TripletTravelActivityAttributes.ContentState) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }

        Task {
            let content = ActivityContent(state: state, staleDate: nil)
            for activity in Activity<TripletTravelActivityAttributes>.activities {
                await activity.update(content)
            }
        }
    }

    func end(state: TripletTravelActivityAttributes.ContentState) {
        Task {
            await endAll(dismissalPolicy: .default, state: state)
        }
    }

    private func endAll(
        dismissalPolicy: ActivityUIDismissalPolicy,
        state: TripletTravelActivityAttributes.ContentState
    ) async {
        let content = ActivityContent(state: state, staleDate: nil)
        for activity in Activity<TripletTravelActivityAttributes>.activities {
            await activity.end(content, dismissalPolicy: dismissalPolicy)
        }
    }
}
