import { useEffect, useState } from 'react'
import { publicProfileService } from '../../services/perfilService.js'

export function ProfileAvatar({ profileId, name, className = '', photoAvailable = true }) {
  const [photoUrl, setPhotoUrl] = useState('')

  useEffect(() => {
    let active = true
    let objectUrl = ''
    setPhotoUrl('')
    if (!profileId || !photoAvailable) return () => { active = false }

    publicProfileService.getPhoto(profileId)
      .then(({ data }) => {
        objectUrl = URL.createObjectURL(data)
        if (active) setPhotoUrl(objectUrl)
        else URL.revokeObjectURL(objectUrl)
      })
      .catch(() => {})

    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [profileId, photoAvailable])

  const initial = name?.trim()?.slice(0, 1).toUpperCase() || 'P'
  return photoUrl
    ? <img alt={`Foto de perfil de ${name || 'usuario'}`} className={`${className} profile-avatar-image`} src={photoUrl} />
    : <span aria-label={photoAvailable ? initial : `Sin foto, ${initial}`} className={className}>{initial}</span>
}