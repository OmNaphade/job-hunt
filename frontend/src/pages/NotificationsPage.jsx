import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import SectionCard from '../components/SectionCard'
import { ErrorMessage, SuccessMessage } from '../components/Message'
import { EmptyState, ListSkeleton } from '../components/StateBlocks'
import { useConfirm } from '../components/ConfirmDialogProvider'
import { useAuth } from '../context/AuthContext'
import { useJobAlertPreference } from '../hooks/useJobAlertPreference'
import { api, getErrorMessage } from '../lib/api'

function asNotificationList(payload) {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

export default function NotificationsPage() {
  const { role } = useAuth()
  const confirm = useConfirm()
  const [createUserId, setCreateUserId] = useState('')
  const [createType, setCreateType] = useState('APPLICATION_UPDATE')
  const [createMessage, setCreateMessage] = useState('')
  const [deleteId, setDeleteId] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [sortOrder, setSortOrder] = useState('NEWEST')
  const [sourceFilter, setSourceFilter] = useState('ALL')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const queryClient = useQueryClient()
  const canManageNotifications = role === 'ADMIN' || role === 'RECRUITER'
  const [jobAlertsEnabled] = useJobAlertPreference()

  const notificationsQuery = useQuery({
    queryKey: ['notifications', sourceFilter],
    queryFn: async () => {
      const endpoint = sourceFilter === 'UNREAD' ? '/api/notifications/unread' : '/api/notifications'
      const response = await api.get(endpoint)
      return asNotificationList(response.data)
    },
  })

  const unreadCountQuery = useQuery({
    queryKey: ['notifications', 'unreadCount'],
    queryFn: async () => {
      const response = await api.get('/api/notifications/unread/count')
      return response.data.count
    },
  })

  const isLoadingNotifications = notificationsQuery.isLoading
  const notifications = notificationsQuery.data || []
  const unreadCount = unreadCountQuery.data

  const markReadMutation = useMutation({
    mutationFn: async (id) => api.patch(`/api/notifications/${id}/read`),
    onMutate: async (id) => {
      setError('')
      setSuccess('')
      await queryClient.cancelQueries({ queryKey: ['notifications'] })
      const previous = queryClient.getQueryData(['notifications', sourceFilter])
      queryClient.setQueryData(['notifications', sourceFilter], (old = []) =>
        asNotificationList(old).map((item) => (item.id === id ? { ...item, status: 'READ' } : item)),
      )
      return { previous }
    },
    onSuccess: () => {
      setSuccess('Marked as read')
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unreadCount'] })
    },
    onError: (err, _id, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['notifications', sourceFilter], context.previous)
      }
      setError(getErrorMessage(err))
    },
  })

  const bulkReadMutation = useMutation({
    mutationFn: async () => api.patch('/api/notifications/read-all'),
    onMutate: async () => {
      setError('')
      setSuccess('')
      await queryClient.cancelQueries({ queryKey: ['notifications'] })
      const previous = queryClient.getQueryData(['notifications', sourceFilter])
      queryClient.setQueryData(['notifications', sourceFilter], (old = []) =>
        asNotificationList(old).map((item) => ({ ...item, status: 'READ' })),
      )
      return { previous }
    },
    onSuccess: (response) => {
      const updated = response?.data?.updated ?? 0
      setSuccess(`Marked ${updated} notifications as read`)
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unreadCount'] })
    },
    onError: (err, _v, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['notifications', sourceFilter], context.previous)
      }
      setError(getErrorMessage(err))
    },
  })

  async function bulkMarkRead() {
    setError('')
    setSuccess('')

    const unreadCountInView = filteredNotifications.filter((item) => item.status !== 'READ').length
    if (unreadCountInView === 0) {
      setSuccess('No unread notifications in current view')
      return
    }

    bulkReadMutation.mutate()
  }

  const createMutation = useMutation({
    mutationFn: async (payload) => api.post('/api/notifications', payload),
    onSuccess: () => {
      setSuccess('Notification created')
      setCreateMessage('')
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unreadCount'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  async function createNotification(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    createMutation.mutate({
      userId: Number(createUserId),
      type: createType,
      message: createMessage,
    })
  }

  const deleteMutation = useMutation({
    mutationFn: async (id) => api.delete(`/api/notifications/${id}`),
    onSuccess: () => {
      setSuccess('Notification deleted')
      setDeleteId('')
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unreadCount'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  async function deleteNotification(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    const confirmed = await confirm({
      title: `Delete notification #${deleteId}?`,
      message: 'This permanently removes the notification. This cannot be undone.',
      confirmLabel: 'Delete notification',
    })
    if (!confirmed) return
    deleteMutation.mutate(deleteId)
  }

  const filteredNotifications = notifications
    .filter((item) => {
      if (!jobAlertsEnabled && item.type === 'JOB_ALERT') return false
      if (statusFilter !== 'ALL' && item.status !== statusFilter) return false
      if (typeFilter !== 'ALL' && item.type !== typeFilter) return false
      return true
    })
    .sort((a, b) => {
      const aTime = new Date(a.createdAt || a.updatedAt || 0).getTime()
      const bTime = new Date(b.createdAt || b.updatedAt || 0).getTime()
      if (sortOrder === 'OLDEST') return aTime - bTime
      return bTime - aTime
    })

  const hiddenJobAlertCount = jobAlertsEnabled
    ? 0
    : notifications.filter((item) => item.type === 'JOB_ALERT').length

  const notificationTypes = Array.from(new Set(notifications.map((item) => item.type).filter(Boolean)))

  return (
    <div className="space-y-6">
      <SectionCard
        title="Notifications Module"
        subtitle="Kafka-driven updates surfaced via notification_service."
        right={
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => {
                setSourceFilter('ALL')
                queryClient.invalidateQueries({ queryKey: ['notifications'] })
              }}
              className="btn btn-accent btn-sm"
            >
              Load Notifications
            </button>
            <button
              type="button"
              onClick={() => queryClient.invalidateQueries({ queryKey: ['notifications', 'unreadCount'] })}
              className="btn btn-warning btn-sm"
            >
              Unread Count
            </button>
            <button
              type="button"
              onClick={() => {
                setSourceFilter('UNREAD')
                queryClient.invalidateQueries({ queryKey: ['notifications'] })
              }}
              className="btn btn-dark btn-sm"
            >
              Unread List
            </button>
            <button
              type="button"
              onClick={bulkMarkRead}
              className="btn btn-success btn-sm"
            >
              Bulk Mark Read
            </button>
          </div>
        }
      >
        <ErrorMessage text={error} />
        <SuccessMessage text={success} />

        {typeof unreadCount === 'number' ? (
          <p className="mt-4 badge badge-dark">
            Unread: {unreadCount}
          </p>
        ) : null}

        <div className="mt-4 grid gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-3 sm:grid-cols-3">
          <select
            className="input"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="UNREAD">UNREAD</option>
            <option value="READ">READ</option>
          </select>
          <select
            className="input"
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}
          >
            <option value="ALL">All Types</option>
            {notificationTypes.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
          <select
            className="input"
            value={sortOrder}
            onChange={(event) => setSortOrder(event.target.value)}
          >
            <option value="NEWEST">Newest First</option>
            <option value="OLDEST">Oldest First</option>
          </select>
        </div>

        {hiddenJobAlertCount > 0 ? (
          <p className="field-hint mt-3">
            {hiddenJobAlertCount} job alert{hiddenJobAlertCount === 1 ? '' : 's'} hidden by your device
            preference &mdash;{' '}
            <Link to="/profile#settings" className="font-semibold text-indigo-600 hover:underline">
              manage in Account Settings
            </Link>
            .
          </p>
        ) : null}

        {isLoadingNotifications ? <ListSkeleton rows={3} /> : null}

        {!isLoadingNotifications && filteredNotifications.length === 0 ? (
          <EmptyState
            title="No notifications loaded"
            message="Adjust filters or load notifications to populate this list."
          />
        ) : null}

        {!isLoadingNotifications && filteredNotifications.length > 0 ? (
          <div className="mt-4 grid gap-3">
            {filteredNotifications.map((item) => (
              <article key={item.id} className="item-card">
                <p className="font-bold text-slate-900">{item.type}</p>
                <p className="mt-1 text-sm text-slate-700">{item.message}</p>
                <p className="mt-1 text-xs text-slate-500">
                  {item.createdAt ? new Date(item.createdAt).toLocaleString() : 'No timestamp available'}
                </p>
                <div className="mt-3 flex items-center justify-between">
                  <span className="text-xs uppercase tracking-wider text-slate-500">{item.status}</span>
                  {item.status !== 'READ' ? (
                    <button
                      type="button"
                      onClick={() => markReadMutation.mutate(item.id)}
                      className="btn btn-success btn-sm"
                    >
                      Mark Read
                    </button>
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        ) : null}

        {canManageNotifications ? (
          <div className="mt-4 grid gap-4 lg:grid-cols-2">
            <form className="item-card grid gap-3" onSubmit={createNotification}>
              <p className="text-sm font-semibold text-slate-700">POST /api/notifications</p>
              <input
                className="input"
                placeholder="Target User ID"
                value={createUserId}
                onChange={(event) => setCreateUserId(event.target.value)}
              />
              <select
                className="input"
                value={createType}
                onChange={(event) => setCreateType(event.target.value)}
              >
                <option value="APPLICATION_UPDATE">APPLICATION_UPDATE</option>
                <option value="JOB_ALERT">JOB_ALERT</option>
                <option value="SYSTEM">SYSTEM</option>
              </select>
              <textarea
                className="input"
                rows={3}
                placeholder="Notification message"
                value={createMessage}
                onChange={(event) => setCreateMessage(event.target.value)}
              />
              <button type="submit" className="btn btn-success">
                Create Notification
              </button>
            </form>

            <form className="item-card grid gap-3" onSubmit={deleteNotification}>
              <p className="text-sm font-semibold text-slate-700">DELETE /api/notifications/{'{id}'}</p>
              <input
                className="input"
                placeholder="Notification ID"
                value={deleteId}
                onChange={(event) => setDeleteId(event.target.value)}
              />
              <button type="submit" className="btn btn-danger">
                Delete Notification
              </button>
            </form>
          </div>
        ) : null}
      </SectionCard>
    </div>
  )
}
